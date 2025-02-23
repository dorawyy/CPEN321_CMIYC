import { Request, Response, NextFunction } from "express";
import { client } from "../services";
import { ObjectId } from "mongodb";

export class UserController {
    // Used to create a user profile with a name, email, profile picture, and status.    
    async createUserProfile(req: Request, res: Response, nextFunction: NextFunction) {
        const createdUserProfile = await client.db("cmiyc").collection("users").insertOne(req.body);
        res.status(200).send("User profile created: " + createdUserProfile.insertedId);
    }

    // Used to get a user profile by its ID.
    async getUserProfile(req: Request, res: Response, nextFunction: NextFunction) {
        const userProfile = await client.db("cmiyc").collection("users").findOne({ _id: new ObjectId(req.params.id) });
        if (userProfile) {
            res.send(userProfile);
        } else {
            res.status(404).send("User profile not found");
        }
    }

    // Used to update user profile information, including name, email, profile picture, and status.
    async updateUserProfile(req: Request, res: Response, nextFunction: NextFunction) {
        const updatedUserProfile = await client.db("cmiyc").collection("users").replaceOne({ _id: new ObjectId(req.params.id) }, req.body);
        if (!updatedUserProfile.acknowledged || updatedUserProfile.modifiedCount == 0) {
            res.status(404).send("User profile not found");
        } else {
            res.send("User profile updated");
        }
    }

    // Used to delete a user profile.
    async deleteUserProfile(req: Request, res: Response, nextFunction: NextFunction) {
        const deletedUser = await client.db("cmiyc").collection("users").deleteOne({ userID: req.params.userID });
        
        if (!deletedUser.acknowledged || deletedUser.deletedCount == 0) {
            res.status(404).send("User not found");
        } else {
            res.send("User deleted");
        }
    }
}