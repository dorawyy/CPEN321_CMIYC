import express, { NextFunction } from 'express';
import { Request, Response } from 'express';
import { MongoClient } from 'mongodb';
import { client } from './services';
import { LocationRoutes } from './routes/LocationRoutes';
import { UserRoutes } from './routes/UserRoutes';
import { FriendRoutes } from './routes/FriendRoutes';
import { validationResult } from 'express-validator';
import morgan from 'morgan';

const app = express();
const port = 3001;

app.use(express.json());
app.use(morgan('tiny'));

const Routes = [...LocationRoutes, ...UserRoutes, ...FriendRoutes];

Routes.forEach((route) => {
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
        await route.action(req, res, next);
      } catch (err) {
        console.log(err);
        return res.sendStatus(500); // Don't expose internal server workings
      }
    }
  );
  });
      

client.connect().then(() => {
    console.log("Connected to MongoDB");
    app.listen(port, () => {
      console.log(`Server started at http://localhost:${port}`);
    });
  }
  ).catch((err) => {
      console.log(err);
      client.close();
  });
