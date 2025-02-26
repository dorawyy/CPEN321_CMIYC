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
            return res.status(200).send("User profile already exists");
        }
        body.friends = [];
        body.friendRequests = [];
        body.logList = [];
        
        const createdUserProfile = await client.db("cmiyc").collection("users").insertOne(body);
        res.status(200).send("User profile created: " + createdUserProfile.insertedId);
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