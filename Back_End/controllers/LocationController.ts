import { Request, Response, NextFunction } from "express";
import { client } from "../services";
import { ObjectId } from "mongodb";

export class LocationController {
    // Used to get all locations of all users.
    async getLocations(req: Request, res: Response, nextFunction: NextFunction) {
        const locations = await client.db("cmiyc").collection("locations").find().toArray();
        res.send(locations);
    }

    // Used to add a location with a user ID and a location object containing latitude and longitude.
    async postUserLocation(req: Request, res: Response, nextFunction: NextFunction) {
        const createdLocation = await client.db("cmiyc").collection("locations").insertOne(req.body);
        res.status(200).send("Location added: " + createdLocation.insertedId);
    }
    
    // Used to get a location by the user ID.
    async getUserLocation(req: Request, res: Response, nextFunction: NextFunction) {
        const location = await client.db("cmiyc").collection("locations").findOne({ userID: req.params.userID });
        if (location) {
            res.send(location);
            
        } else {
            res.status(404).send("Location not found");
        }
    }

    // Used to update a location by the user ID.
    async updateUserLocation(req: Request, res: Response, nextFunction: NextFunction) {
        const updatedLocation = await client.db("cmiyc").collection("locations").replaceOne({ userID: req.params.userID }, req.body);
        if (!updatedLocation.acknowledged || updatedLocation.modifiedCount == 0) {
            res.status(404).send("Location not found");
        } else {
            res.send("Location updated");
        }
    }

    async deleteUserLocation(req: Request, res: Response, nextFunction: NextFunction) {
        const deletedLocation = await client.db("cmiyc").collection("locations").deleteOne({ userID: req.params.userID });
        
        if (!deletedLocation.acknowledged || deletedLocation.deletedCount == 0) {
            res.status(404).send("Location not found");
        } else {
            res.send("Location deleted");
        }
    }
}