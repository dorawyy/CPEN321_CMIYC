import { Request, Response, NextFunction } from "express";
import { client } from "../services";

export class FriendController {
    // Used to get a user's friend list. GET request.
    async getFriends(req: Request, res: Response, nextFunction: NextFunction) {
        const userID = req.params.userID;
        const user = await client.db("cmiyc").collection("users").findOne({ userID });
        if (user) {
            res.send(user.friends);
        } else {
            res.status(404).send("User not found");
        }
    }
    
    // Used to send a friend request to a user. POST request.
    async sendFriendRequest(req: Request, res: Response, nextFunction: NextFunction) {
        const { userID, friendID } = req.body;

        if (userID === friendID) {
            res.status(400).send("Cannot send friend request to yourself");
            return;
        }

        try {
            const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });
            const friend = await client.db("cmiyc").collection("users").findOne({ friendID: friendID });

            if (!user || !friend) {
                return res.status(404).send("User or friend not found");
            }

            // Check if already friends
            if (user.friends.includes(friendID)) {
                return res.status(400).send("You are already friends");
            }

            // Check if blocked
            if (user.blocked.includes(friendID)) {
                return res.status(400).send("You have blocked this user");
            }

            // Send friend request
            await client.db("cmiyc").collection("users").updateOne({ userID: friendID }, { $push: { friendRequests: userID } });
            
            res.status(200).send("Friend request sent successfully");

        } catch (err) {
            res.status(404).send("Error sending friend request");
        }
    }

    // Used to respond to a friend request. POST request.
    async respondToFriendRequest(req: Request, res: Response, nextFunction: NextFunction) {
        const { userID, friendID, response } = req.body;

        try {
            const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });
            const friend = await client.db("cmmiyc").collection("users").findOne({ friendID: friendID });

            if (!user || !friend) {
                return res.status(404).send("User or friend not found");
            }

            // Check if friend request exists
            if (!friend.friendRequests.includes(userID)) {
                return res.status(400).send("No friend request found");
            }

            // Accept friend request - adds friend to both users' friend lists
            if (response === "accept") {
                await client.db("cmiyc").collection("users").updateOne({ userID: userID }, { $push: { friends: friendID } });
                await client.db("cmiyc").collection("users").updateOne({ userID: friendID }, { $push: { friends: userID } });
            }

            // Decline friend request
            await client.db("cmiyc").collection("users").updateOne({ userID: friendID }, { $pull: { friendRequests: userID } });
            
            res.status(200).send("Friend request responded to successfully");

        } catch (err) {
            res.status(404).send("Error responding to friend request");
        }
    }

    // Used to block a user.
    async blockUser(req: Request, res: Response, nextFunction: NextFunction) {
        const { userID, friendID } = req.body;

        if (userID === friendID) {
            res.status(400).send("Cannot block yourself");
            return;
        }

        try {
            const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });
            const friend = await client.db("cmiyc").collection("users").findOne({ friendID: friendID });

            if (!user || !friend) {
                return res.status(404).send("User or friend not found");
            }

            // Check if already blocked
            if (user.blocked.includes(friendID)) {
                return res.status(400).send("You have already blocked this user");
            }

            // Block user
            await client.db("cmiyc").collection("users").updateOne({ userID: userID }, { $push: { blocked: friendID } });
            
            res.status(200).send("User blocked successfully");

        } catch (err) {
            res.status(404).send("Error blocking user");
        }
    }

    // Used to delete a user's friend. DELETE request.
    async removeFriend(req: Request, res: Response, nextFunction: NextFunction) { 
        const { userID, friendID } = req.params;

        try {
            const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });
            
            if (!user) {
                return res.status(404).send("User not found");
            }

            // Remove friend from users' friend lists
            user.friends = user.friends.filter((id: string) => id !== friendID);
            await user.save();

            res.status(200).send("Friend deleted successfully");
        
        } catch (err) {
            res.status(404).send("Error deleting friend");
        }
    }

    // Used to get a user's nearby friends. GET request.
    async getNearbyFriends(req: Request, res: Response, nextFunction: NextFunction) {
        const { userID, location, radius} = req.body;

        try {
            const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });
            if (!user) {
                return res.status(404).send("User not found");
            }

            // Find nearby friends
            const nearbyFriends = await client.db("cmiyc").collection("users").find({
                location: {
                    $near: {
                        $geometry: {
                            type: "Point",
                            coordinates: [location.longitude, location.latitude]
                        },
                        $maxDistance: radius
                    }
                }
            }).toArray();

            res.status(200).send(nearbyFriends);
        }
        catch (err) {
            res.status(404).send("Error getting nearby friends");
        }
    }
}