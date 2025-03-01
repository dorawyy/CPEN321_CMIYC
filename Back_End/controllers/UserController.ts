import { Request, Response, NextFunction } from "express";
import { client } from "../services";
import { ObjectId } from "mongodb";

export class UserController {
    // Used to create a user profile with a name, email, profile picture, and status.    
    async createUserProfile(req: Request, res: Response, nextFunction: NextFunction) {
        const body = req.body;
        // Check if user already exists
        const existingUser = await client.db("cmiyc").collection("users").findOne({ userID: body.userID });
        if (existingUser) {
            if (body.email == "kdeepan240@gmail.com") {
                return res.status(200).send({ message: "User profile already exists", isAdmin: true, isBanned: false });
            }
            if (existingUser.isBanned) {
                return res.status(200).send({ message: "User is banned", isAdmin: false, isBanned: true });
            }
            return res.status(200).send({ message: "User profile already exists", isAdmin: false, isBanned: false });
        }
        body.friends = [];
        body.friendRequests = [];
        body.logList = [];
        body.isBanned = false;
        if (body.email == "kdeepan240@gmail.com") {
            body.isAdmin = true;
        } else {
            body.isAdmin = false;
        }
        
        const createdUserProfile = await client.db("cmiyc").collection("users").insertOne(body);
        res.status(200).send("User profile created: " + createdUserProfile.insertedId);
    }

    async getAllUsers(req: Request, res: Response, nextFunction: NextFunction) {
        if (req.params.userID != "kdeepan240@gmail.com") {
            return res.status(403).send("Unauthorized");
        }
        const users = await client.db("cmiyc").collection("users").find({}).toArray();
        const usersWithoutLists = users.map((user) => {
            const { friends: _, friendRequests: __, ...userWithoutLists } = user;
            return userWithoutLists;
        });
        res.status(200).send(usersWithoutLists);
    }

    async banUser(req: Request, res: Response, nextFunction: NextFunction) {
        const adminID = req.body.adminID;
        if (adminID != "kdeepan240@gmail.com") {
            return res.status(403).send("Unauthorized");
        }
        const userID = req.params.userID;
        const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });
        if (!user) {
            return res.status(404).send("User not found");
        }
        await client.db("cmiyc").collection("users").updateOne({ userID: userID }, { $set: { isBanned: true } });
        res.status(200).send("User banned");
    }



    // Used to delete a user profile.
    // async deleteUserProfile(req: Request, res: Response, nextFunction: NextFunction) {
    //     const deletedUser = await client.db("cmiyc").collection("users").deleteOne({ userID: req.params.userID });
        
    //     if (!deletedUser.acknowledged || deletedUser.deletedCount == 0) {
    //         res.status(404).send("User not found");
    //     } else {
    //         res.send("User deleted");
    //     }
    // }
}