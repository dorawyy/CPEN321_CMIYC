import express, { NextFunction } from 'express';
import { Request, Response } from 'express';
import { client } from './services';
import { LocationRoutes } from './routes/LocationRoutes';
import { UserRoutes } from './routes/UserRoutes';
import { FriendRoutes } from './routes/FriendRoutes';
import { NotificationRoutes } from './routes/NotificationRoutes';
import { validationResult } from 'express-validator';
import morgan from 'morgan';
import https from 'https';
import fs from 'fs';

const app = express();
const port = Number(process.env.PORT) || 80;
const httpsPort = 443;

app.use(express.json());
app.use(morgan('tiny'));

const Routes = [...LocationRoutes, ...UserRoutes, ...FriendRoutes, ...NotificationRoutes];

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

// HTTPS options
const httpsOptions = {
  key: fs.readFileSync('/app/ssl/server.key'),
  cert: fs.readFileSync('/app/ssl/server.key')
};

client.connect().then(() => {
    console.log("Connected to MongoDB");
    
    // Start HTTP server
    app.listen(port, '0.0.0.0', () => {
      console.log(`HTTP Server started at http://localhost:${port}`);
    });
    
    // Start HTTPS server
    https.createServer(httpsOptions, app).listen(httpsPort, '0.0.0.0', () => {
      console.log(`HTTPS Server started at https://localhost:${httpsPort}`);
    });
  }
).catch((err) => {
    console.log(err);
    client.close();
});