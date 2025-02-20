import express, { NextFunction } from 'express';
import { Request, Response } from 'express';
import { MongoClient } from 'mongodb';
import { client } from './services';
import { LocationRoutes } from './routes/LocationRoutes';
import morgan from 'morgan';

const app = express();
const port = 3000;

app.use(express.json());
app.use(morgan('tiny'));

LocationRoutes.forEach((route) => {
    (app as any)[route.method](
      route.route,
      async (req: Request, res: Response, next: NextFunction) => {
        try {
          await route.action(req, res, next);
        } catch (err) {
          console.log(err);
          return res.sendStatus(500);
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
