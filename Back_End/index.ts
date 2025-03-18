import express, { Request, Response } from 'express';
import { client } from './services';
import { LocationRoutes } from './routes/LocationRoutes';
import { UserRoutes } from './routes/UserRoutes';
import { FriendRoutes } from './routes/FriendRoutes';
import { NotificationRoutes } from './routes/NotificationRoutes';
import { validationResult } from 'express-validator';
import morgan from 'morgan';

const app = express();
const port = Number(process.env.PORT) || 80;

app.use(express.json());
app.use(morgan('tiny'));

const Routes = [...LocationRoutes, ...UserRoutes, ...FriendRoutes, ...NotificationRoutes];

Routes.forEach((route) => { 
  (app[route.method as keyof typeof app])(
    route.route,
    route.validation,
    async (req: Request, res: Response) => {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        /* If there are validation errors, send a response with the error messages */
        return res.status(400).send({ errors: errors.array() });
      }
      try {
        await route.action(req, res);
      } catch (err) {
        // Avoid logging the full error object for security reasons
        console.log('Server error occurred');
        return res.sendStatus(500); // Don't expose internal server workings
      }
    }
  );
  });
      

client.connect().then(() => {
    console.log("Connected to MongoDB");
    app.listen(port, '0.0.0.0', () => {
      // Use a static message without exposing port in logs
      console.log("Server started successfully");
    });
  }
  ).catch(() => {
      console.log('Database connection error');
      client.close();
  });
