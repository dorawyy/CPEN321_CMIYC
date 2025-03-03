import { UserController } from "../controllers/UserController";
import { body } from "express-validator";

const controller = new UserController();

export const UserRoutes = [
    {
        method: "post",
        route: "/user",
        action: controller.createUserProfile,
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
        action: controller.getAllUsers,
        validation: []
    },

    {
        method: "post",
        route: "/user/ban/:userID",
        action: controller.banUser,
        validation: []
    }
]