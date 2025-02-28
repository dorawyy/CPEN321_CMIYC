import { FriendController } from "../controllers/FriendController";
import { body } from "express-validator";

const controller = new FriendController();

export const FriendRoutes = [
    {
        method: "post",
        route: "/user/:userID/friends/:friendID",
        action: controller.sendFriendRequest,
        validation: [
            body("userID").notEmpty().isString(),
            body("friendID").notEmpty().isString()
        ]
    },
    {
        method: "post",
        route: "/user/:userID/friends/:friendID/block",
        action: controller.blockUser,
        validation: [
            body("userID").notEmpty().isString(),
            body("friendID").notEmpty().isString()
        ]
    },
    {
        method: "post",
        route: "/user/:userID/friendRequests/:friendID",
        action: controller.respondToFriendRequest,
        validation: [
            body("userID").notEmpty().isString(),
            body("friendID").notEmpty().isString(),
            body("response").notEmpty().isString()
        ]
    },
    {
        method: "get",
        route: "/user/:userID/friends",
        action: controller.getFriends,
        validation: []
    },
    {
        method: "delete",
        route: "/user/:userID/friends/:friendID",
        action: controller.removeFriend,
        validation: [
            body("userID").notEmpty().isString(),
            body("friendID").notEmpty().isString()
        ]
    },
    {
        method: "get",
        route: "/user/:userID/getNearbyFriends",
        action: controller.getNearbyFriends,
        validation: [
            body("userID").notEmpty().isString(),
            body("location").notEmpty(),
            body("radius").notEmpty()
        ]
    }
]