import { Request, Response, NextFunction } from "express";
import { client } from "../services";

export class LocationController {
    // Used to update a location by the user ID.
    async updateUserLocation(req: Request, res: Response, nextFunction: NextFunction) {
        const userID = req.params.userID;
        const currentLocation = req.body.currentLocation;
        console.log(userID);

        // Check if user exists
        const user = await client.db("cmiyc").collection("users").findOne({ userID: userID });
        if (!user) {
            res.status(404).send("User not found");
            return;
        }

        // Update user location
        await client.db("cmiyc").collection("users").updateOne({ userID: userID }, { $set: { currentLocation: currentLocation } });
        res.send("Location updated");
    }
}