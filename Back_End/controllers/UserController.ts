import { Request, Response } from "express";
import { client } from "../services";

export class UserController {
    // Used to create a user profile with a name, email, profile picture, and status.    
    async createUserProfile(req: Request, res: Response) {
        const body = req.body;
        // Check if user already exists
        const existingUser = await client.db("cmiyc").collection("users").findOne({ userID: body.userID });
        if (existingUser) {
            if (existingUser.isBanned) {
                return res.status(200).send({ message: "User is banned", isAdmin: body.isAdmin, isBanned: true });
            }
            return res.status(200).send({ message: "User profile already exists", isAdmin: body.isAdmin, isBanned: false });
        }
        body.logList = [];
        body.friends = [];
        body.friendRequests = [];
        body.isBanned = false;        
        const createdUserProfile = await client.db("cmiyc").collection("users").insertOne(body);
        res.status(200).send({ message: "User profile created: " + createdUserProfile.insertedId, isAdmin: body.isAdmin, isBanned: false});
    }

    async getAllUsers(req: Request, res: Response) {
        const userID = req.params.userID;
        const users = await client.db("cmiyc").collection("users").find({}).toArray();
        const usersWithoutLists = users
            .filter(user => user.userID !== userID)
            .map((user) => {
                const { friendRequests, ...userWithoutLists } = user;
                // Ensure these properties are undefined rather than empty arrays
                delete userWithoutLists.friends;
                delete userWithoutLists.friendRequests;
                return userWithoutLists;
            });
        res.status(200).send(usersWithoutLists);
    }

    async banUser(req: Request, res: Response) {
        const userID = req.params.userID;
        const user = await client.db("cmiyc").collection("users").findOne({ userID });
        if (!user) {
            return res.status(404).send("User not found");
        }
        await client.db("cmiyc").collection("users").updateOne({ userID }, { $set: { isBanned: true } });
        res.status(200).send("User banned");
    }
}