import { UserController } from "../controllers/UserController";
import { body } from "express-validator";
import { Request, Response } from "express";

const controller = new UserController();

export const UserRoutes = [
    {
        method: "post",
        route: "/user",
        action: (req: Request, res: Response) => controller.createUserProfile(req, res),
        validation: [
            body("userID").notEmpty().isString(),
            body("displayName").notEmpty().isString(),
            body("email").notEmpty().isString().isEmail(),
            body("photoURL").notEmpty().isString(),
            body("fcmToken").notEmpty().isString(),
            body("currentLocation").notEmpty().isObject(), // { latitude: number, longitude: number, timestamp: number }
            body("isAdmin").notEmpty().isBoolean(),
        ]
    },

    {
        method: "get",
        route: "/user/:userID",
        action: (req: Request, res: Response) => controller.getAllUsers(req, res),
        validation: []
    },

    {
        method: "post",
        route: "/user/ban/:userID",
        action: (req: Request, res: Response) => controller.banUser(req, res),
        validation: []
    }
]