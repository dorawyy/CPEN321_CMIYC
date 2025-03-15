import { Express } from 'express';
import request from 'supertest';
import express from 'express';
import { json } from 'express';
import { validationResult } from 'express-validator';
import { UserRoutes } from '../routes/UserRoutes';
import { NotificationRoutes } from '../routes/NotificationRoutes';
import { FriendRoutes } from '../routes/FriendRoutes';
import { LocationRoutes } from '../routes/LocationRoutes';
import dotenv from 'dotenv';

// Load environment variables from .env file for tests
dotenv.config();

// Create an Express application for testing
export function setupTestApp(): Express {
  const app = express();
  app.use(json());
  
  // Middleware to handle validation
  const validationMiddleware = (req: any, res: any, next: any) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }
    next();
  };

  // Register UserRoutes
  UserRoutes.forEach(route => {
    (app as any)[route.method](
      route.route,
      ...route.validation,
      validationMiddleware,
      (req: any, res: any, next: any) => {
        const result = route.action(req, res, next);
        if (result instanceof Promise) {
          result.catch(err => next(err));
        }
      }
    );
  });

  // Register NotificationRoutes
  NotificationRoutes.forEach(route => {
    (app as any)[route.method](
      route.route,
      ...route.validation,
      validationMiddleware,
      (req: any, res: any, next: any) => {
        const result = route.action(req, res, next);
        if (result instanceof Promise) {
          result.catch(err => next(err));
        }
      }
    );
  });

  // Register FriendRoutes
  FriendRoutes.forEach(route => {
    (app as any)[route.method](
      route.route,
      ...route.validation,
      validationMiddleware,
      (req: any, res: any, next: any) => {
        const result = route.action(req, res, next);
        if (result instanceof Promise) {
          result.catch(err => next(err));
        }
      }
    );
  });

  // Register LocationRoutes
  LocationRoutes.forEach(route => {
    (app as any)[route.method](
      route.route,
      ...route.validation,
      validationMiddleware,
      (req: any, res: any, next: any) => {
        const result = route.action(req, res, next);
        if (result instanceof Promise) {
          result.catch(err => next(err));
        }
      }
    );
  });

  return app;
}

// Helper to create a test request
export function createTestRequest(app: Express) {
  return request(app);
}

// Sample test data for users
export const testUserData = {
  userID: "test-user-123",
  displayName: "Test User",
  email: "test@example.com",
  photoURL: "https://example.com/photo.jpg",
  fcmToken: "test-fcm-token-123",
  currentLocation: { latitude: 49.2827, longitude: -123.1207, timestamp: Date.now() },
  isAdmin: false
}; 