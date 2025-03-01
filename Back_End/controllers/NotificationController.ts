import { Request, Response, NextFunction } from "express";
import { client, messaging } from "../services";
import { Quadtree, Point, Rectangle } from "../utils/Quadtree";

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

    // Find nearby friends using quadtree
    private static findNearbyFriendsWithQuadtree(user: any, friends: any[]): any[] {
        // Create a quadtree covering the entire world
        // Using longitude (-180 to 180) and latitude (-90 to 90)
        const worldBoundary: Rectangle = {
            x: 0,      // center longitude
            y: 0,      // center latitude
            width: 180, // half the longitude range
            height: 90  // half the latitude range
        };
        
        const quadtree = new Quadtree(worldBoundary);
        
        // Insert all friends into the quadtree
        for (const friend of friends) {
            if (!friend.currentLocation) continue;
            
            const point: Point = {
                x: friend.currentLocation.longitude,
                y: friend.currentLocation.latitude,
                data: friend
            };
            
            quadtree.insert(point);
        }
        
        // Define search range (1km around user)
        // Convert 1km to approximate degrees (very rough approximation)
        // 1 degree of latitude is approximately 111km
        // 1 degree of longitude varies with latitude
        const latDegree = 1 / 111; // 1km in latitude degrees
        
        // Longitude degrees per km varies with latitude
        // cos(latitude) * 111km
        const lonDegree = 1 / (111 * Math.cos(user.currentLocation.latitude * Math.PI / 180));
        
        const searchRange: Rectangle = {
            x: user.currentLocation.longitude,
            y: user.currentLocation.latitude,
            width: lonDegree,
            height: latDegree
        };
        
        // Query the quadtree for points within the search range
        const nearbyPoints = quadtree.query(searchRange);
        
        // Extract the friend data from the points
        const nearbyFriends = nearbyPoints.map(point => point.data);

        
        console.log("Nearby friends from quadtree:", nearbyFriends);
        return nearbyFriends;
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

            // Use quadtree to find nearby friends
            const nearbyFriends = NotificationController.findNearbyFriendsWithQuadtree(user, friends);

            for (const friend of nearbyFriends) {
                // First, ensure the logList exists (using $set with empty array if it doesn't)
                await collection.updateOne(
                    { userID: friend.userID, logList: { $exists: false } },
                    { $set: { logList: [] } }
                );
                
                // Then push the new notification to the logList
                await collection.updateOne(
                    { userID: friend.userID },
                    { 
                        $push: { 
                            logList: {
                                fromName: user.displayName,
                                eventName: req.body.eventName,
                                location: user.currentLocation,
                            }
                        } 
                    } as any
                );
            }

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
}
