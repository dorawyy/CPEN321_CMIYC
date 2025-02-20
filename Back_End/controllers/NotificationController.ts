import { Request, Response, NextFunction } from "express";
import { client } from "../services";
import { ObjectId } from "mongodb";

export class NotificationController {
    async getNotifications(req: Request, res: Response, nextFunction: NextFunction) {
        const notifications = await client.db("cmiyc").collection("notifications").find().toArray();
        res.send(notifications);
    }
}