import { LocationController } from "../controllers/LocationController";
import { body } from "express-validator";
import { Request, Response } from "express";

const controller = new LocationController();

export const LocationRoutes = [
    {
        method: "put",
        route: "/location/:userID",
        action: (req: Request, res: Response) => controller.updateUserLocation(req, res),
        validation: [
            body("currentLocation").notEmpty().isObject() // { latitude: number, longitude: number, timestamp: number }
        ]
    },
]