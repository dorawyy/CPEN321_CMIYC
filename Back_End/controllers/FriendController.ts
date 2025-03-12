import { Request, Response, NextFunction } from "express";
import { client } from "../services";

export class FriendController {

    async getFriends(req: Request, res: Response) {
        const userID = req.params.userID;
        const user = await client.db("cmiyc").collection("users").findOne({ userID });
        if (user) {
            const friends = [];
            for (const friendID of user.friends) {
                const friend = await client.db("cmiyc").collection("users").findOne({ userID: friendID });
                if (friend) {
                    // Create a new object without friends and friendRequests
                    const { friends: _, friendRequests: __, ...friendWithoutLists } = friend;
                    friends.push(friendWithoutLists);
                }
            }
            res.send(friends);

        } else {
            res.status(404).send("User not found");
        }
    }
    
    
    // Used to send a friend request to a user. POST request.
    async sendFriendRequest(req: Request, res: Response) {
        const { userID, friendEmail } = req.params;

        const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });
        
        if (user && user.email === friendEmail) {
            res.status(400).send("Cannot send friend request to yourself");
            return;
        }

        const friend = await client.db("cmiyc").collection("users").findOne({ email: friendEmail });

        if (!friend) {
            res.status(404).send("Friend not found");
            return;
        }

        // Check if already friends
        if (user && user.friends.includes(friend.userID)) {
            res.status(400).send("You are already friends");
            return;
        }

        if (friend.friendRequests.includes(userID)) {
            res.status(200).send("You have already sent a friend request to this user");
            return;
        }

        await client.db("cmiyc").collection("users").updateOne(
            { userID: friend.userID }, 
            { $push: { friendRequests: userID } } as any
        );
        res.status(200).send("Friend request sent successfully");
    }

    // Used to get a user's friend requests. GET request.
    async getFriendRequests(req: Request, res: Response, nextFunction: NextFunction) {
        const { userID } = req.params;
        const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });
        if (user) {
            // Get the full user objects for each friend request
            const friendRequests = await Promise.all(
                user.friendRequests.map(async (requestID: string) => {
                    const friend = await client.db("cmiyc").collection("users").findOne({ userID: requestID });
                    if (friend) {
                        const { friends: _, friendRequests: __, ...friendWithoutLists } = friend;
                        return friendWithoutLists;
                    }
                    // return null;
                })
            );
            res.send(friendRequests.filter(Boolean));
        } else {
            res.status(404).send("User not found");
        }
    }



    // Used to respond to a friend request. POST request.
    async acceptFriendRequest(req: Request, res: Response) {
        const { userID, friendID } = req.params;

        console.log(userID, friendID);

        try {
            const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });
            const friend = await client.db("cmiyc").collection("users").findOne({ userID: friendID });

            console.log(user, friend);

            if (!user || !friend) {
                return res.status(404).send("User or friend not found");
            }

            // Check if friend request exists
            if (!user.friendRequests.includes(friendID)) {
                return res.status(400).send("No friend request found");
            }

            // Accept friend request - adds friend to both users' friend lists
            await client.db("cmiyc").collection("users").updateOne({ userID: userID }, { $push: { friends: friendID } } as any);
            await client.db("cmiyc").collection("users").updateOne({ userID: friendID }, { $push: { friends: userID } } as any);
            

            // Remove friend request
            await client.db("cmiyc").collection("users").updateOne({ userID: userID }, { $pull: { friendRequests: friendID } } as any);
            
            res.status(200).send("Friend request responded to successfully");

        } catch (err) {
            res.status(404).send("Error responding to friend request");
        }
    }

    // Used to decline a friend request. POST request.
    async declineFriendRequest(req: Request, res: Response, nextFunction: NextFunction) {
        const { userID, friendID } = req.params;

        try {
            const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });

            if (!user) {
                return res.status(404).send("User not found");
            }

            // Remove friend request
            await client.db("cmiyc").collection("users").updateOne({ userID: userID }, { $pull: { friendRequests: friendID } } as any);
            
            res.status(200).send("Friend request declined successfully");

        } catch (err) {
            res.status(404).send("Error declining friend request");
        }
    }

    // Used to remove a friend. POST request.
    async deleteFriend(req: Request, res: Response, nextFunction: NextFunction) {
        const { userID, friendID } = req.params;

        try {
            const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });
            const friend = await client.db("cmiyc").collection("users").findOne({ userID: friendID });

            if (!user || !friend) {
                return res.status(404).send("User or friend not found");
            }   

            // Remove friend from users' friend lists
            await client.db("cmiyc").collection("users").updateOne({ userID: userID }, { $pull: { friends: friendID } } as any);
            await client.db("cmiyc").collection("users").updateOne({ userID: friendID }, { $pull: { friends: userID } } as any);
            
            res.status(200).send("Friend deleted successfully");

        } catch (err) {
            res.status(404).send("Error deleting friend");
        }
    }
    
}