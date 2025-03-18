import { FriendController } from "../controllers/FriendController";
import { Request, Response } from "express";

const controller = new FriendController();

export const FriendRoutes = [
    {
        method: "get",
        route: "/friends/:userID",
        action: (req: Request, res: Response) => controller.getFriends(req, res),
        validation: []
    },

    {
        method: "post",
        route: "/friends/:userID/sendRequest/:friendEmail",
        action: (req: Request, res: Response) => controller.sendFriendRequest(req, res),
        validation: []
    },


    {
        method: "get",
        route: "/friends/:userID/friendRequests",
        action: (req: Request, res: Response) => controller.getFriendRequests(req, res),
        validation: []
    },

    {
        method: "post",
        route: "/friends/:userID/acceptRequest/:friendID",
        action: (req: Request, res: Response) => controller.acceptFriendRequest(req, res),
        validation: []
    },

    {
        method: "post",
        route: "/friends/:userID/declineRequest/:friendID",
        action: (req: Request, res: Response) => controller.declineFriendRequest(req, res),
        validation: []
    },

    {
        method: "put",
        route: "/friends/:userID/deleteFriend/:friendID",
        action: (req: Request, res: Response) => controller.deleteFriend(req, res),
        validation: []
    },
    
]