import { Request, Response, NextFunction } from "express";
import { client, messaging } from "../services";

export class NotificationController {
    async setFCMToken(req: Request, res: Response, nextFunction: NextFunction) {
        const fcmToken = req.body.fcmToken;
        const userID = req.params.userID;
        const collection = client.db("cmiyc").collection("users");
        
        // Check if user exists
        const user = await collection.findOne({ userID });
        
        if (!user) {
            return res.status(404).send({ message: "User not found" });
        }
        
        // Update FCM token for existing user
        const result = await collection.updateOne(
            { userID },
            { $set: { fcmToken } }
        );

        if (result.matchedCount === 0) {
            return res.status(404).send({ message: "Failed to update FCM token" });
        }

        res.status(200).send({
            message: "FCM token set successfully"
        });
    }


    //notifications still have to be added to the database
    async sendEventNotification(req: Request, res: Response, nextFunction: NextFunction) {
        const userID = req.params.userID;
        const eventName = req.body.eventName;
        const collection = client.db("cmiyc").collection("users");
        
        try {
            const user = await collection.findOne({ userID });
            
            if (!user) {
                return res.status(404).send({ message: "User not found" });
            }

            if (!user.currentLocation) {
                return res.status(400).send({ message: "User location not set" });
            }

            // Get all friends' documents
            const friends = await collection.find({ 
                userID: { $in: user.friends || [] }
            }).toArray();

            // Calculate distances
            const nearbyFriends = friends.filter((friend) => {
                if (!friend.currentLocation) return false;
                
                const distance = NotificationController.calculateDistance(
                    user.currentLocation.latitude,
                    user.currentLocation.longitude,
                    friend.currentLocation.latitude,
                    friend.currentLocation.longitude
                );
                
                return distance <= 1; // 1 kilometer
            });

            // Send notifications to nearby friends
            const notificationPromises = nearbyFriends.map(friend => {
                if (!friend.fcmToken) return Promise.resolve();
                
                return messaging.send({
                    token: friend.fcmToken,
                    notification: {
                        title: eventName,
                        body: `${user.displayName} is starting a new event!`
                    }
                });
            });

            await Promise.all(notificationPromises);

            //add notifications to logList in nearbyFriends

            console.log(nearbyFriends);
            for (const friend of nearbyFriends) {
                await collection.updateOne(
                    { userID: friend.userID },
                    { 
                        $push: { 
                            logList: {
                                fromName: user.displayName,
                                eventName: req.body.eventName,
                                location: user.currentLocation,
                                timestamp: new Date()
                            }
                        } as any
                    }
                );
            }

            res.status(200).send({
                message: "Notification sent successfully"
            });
            
        } catch (error) {
            console.error(error);
            res.status(500).send({ error: (error as Error).message });
        }
    }

    async getNotifications(req: Request, res: Response, nextFunction: NextFunction) {
        const userID = req.params.userID;
        const collection = client.db("cmiyc").collection("users");
        const user = await collection.findOne({ userID });
        if (!user) {
            return res.status(404).send({ message: "User not found" });
        }
        res.status(200).send(user.logList);
    }

    private static toRadians(degrees: number): number {
        return degrees * (Math.PI/180);
    }

    private static calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
        const R = 6371; // Earth radius in kilometers
        const dLat = NotificationController.toRadians(lat2 - lat1);
        const dLon = NotificationController.toRadians(lon2 - lon1);
        
        const a = 
            Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(NotificationController.toRadians(lat1)) * Math.cos(NotificationController.toRadians(lat2)) *
            Math.sin(dLon/2) * Math.sin(dLon/2);
            
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
        return R * c;
    }
}
