import { LocationController } from "../controllers/LocationController";
import { body } from "express-validator";

const locationController = new LocationController();

export const LocationRoutes = [
    {
        method: "put",
        route: "/location/:userID",
        controller: locationController,
        action: locationController.updateUserLocation,
        validation: [
            body("currentLocation").notEmpty().isObject() // { latitude: number, longitude: number, timestamp: number }
        ]
    },
]