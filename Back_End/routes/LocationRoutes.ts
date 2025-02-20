import {LocationController} from "../controllers/LocationController";

const controller = new LocationController();

export const LocationRoutes = [
    {
        method: "get",
        route: "/location",
        action: controller.getLocations,
    },
    {
        method: "post",
        route: "/location",
        action: controller.postLocation,
        validation: []
    },
    {
        method: "get",
        route: "/location/:id",
        action: controller.getLocation,
    },
    {
        method: "put",
        route: "/location/:id",
        action: controller.updateLocation,
    },
    {
        method: "delete",
        route: "/location/:id",
        action: controller.deleteLocation,
    },
    {
        method: "get",
        route: "/location/:id/friends",
        action: controller.getNearbyFriends,
    }
]