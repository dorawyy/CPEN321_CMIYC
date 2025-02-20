import { Request, Response, NextFunction } from "express";
import { client } from "../services";
import { ObjectId } from "mongodb";

export class LocationController {
    async getLocations(req: Request, res: Response, nextFunction: NextFunction) {
        const locations = await client.db("cmiyc").collection("locations").find().toArray();
        res.send(locations);
    }

    async postLocation(req: Request, res: Response, nextFunction: NextFunction) {
        const createdLocation = await client.db("cmiyc").collection("locations").insertOne(req.body);
        res.status(200).send("Location added: " + createdLocation.insertedId);
    }
    
    async getLocation(req: Request, res: Response, nextFunction: NextFunction) {
        const location = await client.db("cmiyc").collection("locations").findOne({ _id: new ObjectId(req.params.id) });
        if (location) {
            res.send(location);
            
        } else {
            res.status(404).send("Location not found");
        }
    }

    async updateLocation(req: Request, res: Response, nextFunction: NextFunction) {
        const updateLocation = await client.db("cmiyc").collection("locations").replaceOne({ _id: new ObjectId(req.params.id) }, req.body);
        if (!updateLocation.acknowledged || updateLocation.modifiedCount == 0) {
            res.status(404).send("Location not found");
        } else {
            res.send("Location updated");
        }
    }

    async deleteLocation(req: Request, res: Response, nextFunction: NextFunction) {
        const deleteLocation = await client.db("cmiyc").collection("locations").deleteOne({ _id: new ObjectId(req.params.id) });
        
        if (!deleteLocation.acknowledged || deleteLocation.deletedCount == 0) {
            res.status(404).send("Location not found");
        } else {
            res.send("Location deleted");
        }
    }

    async getNearbyFriends(req: Request, res: Response, nextFunction: NextFunction) {
        const location = await client.db("cmiyc").collection("locations").findOne({ id: req.params.locationId });
        if (location) {
            const friends = await client.db("cmiyc").collection("locations").find({
                location: {
                    $near: {
                        $geometry: location.location,
                        $maxDistance: 1000
                    }
                }
            }).toArray();
            res.send(friends);
        } else {
            res.status(404).send("Location not found");
        }
    }
}