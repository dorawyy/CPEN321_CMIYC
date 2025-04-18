import { Request, Response } from "express";
import { client } from "../services";

export class LocationController {
    // Used to update a location by the user ID.
    async updateUserLocation(req: Request, res: Response) {
        const userID = req.params.userID;
        const currentLocation = req.body.currentLocation;

        // Check if user exists
        const user = await client.db("cmiyc").collection("users").findOne({ userID });
        if (!user) {
            res.status(404).send("User not found");
            return;
        }

        // Update user location
        await client.db("cmiyc").collection("users").updateOne({ userID }, { $set: { currentLocation } });
        res.send("Location updated");
    }
}