import { LocationController } from "../controllers/LocationController";
import { body } from "express-validator";

const controller = new LocationController();

export const LocationRoutes = [
    {
        method: "put",
        route: "/location/:userID",
        action: controller.updateUserLocation,
        validation: [
            body("currentLocation").notEmpty().isObject() // { latitude: number, longitude: number, timestamp: number }
        ]
    },
]