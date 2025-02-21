import express, { NextFunction, Request, Response } from "express";
import { client } from "./services";
import { validationResult } from "express-validator";
import { serverRoutes } from "./routes/serverRoutes";
import morgan from "morgan";

const app = express();

app.use(express.json());
app.use(morgan("tiny"));

serverRoutes.forEach((route) => {
    (app as any)[route.method](
        route.route,
        route.validation,
        async (req: Request, res: Response, next: NextFunction) => {
            const errors = validationResult(req);
            if (!errors.isEmpty()) {
                /* If there are validation errors, send a response with the error messages */
                return res.status(400).send({ errors: errors.array() });
            }
            try {
                await route.action(
                    req,
                    res,
                    next,
                );
            } catch (err) {
                console.log(err)
                return res.sendStatus(500); // Don't expose internal server workings
            }
        },
    );
});



client.connect().then(() => {
    console.log("Connected to MongoDB");
    
    app.listen(process.env.PORT, () => {
        console.log(`Server is running on port ${process.env.PORT}`);

    });
}).catch((err) => {
    console.error(err);
    client.close();
}); 