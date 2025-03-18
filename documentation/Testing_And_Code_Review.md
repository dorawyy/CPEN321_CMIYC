# M5: Testing and Code Review

## 1. Change History

| **Change Date**   | **Modified Sections** | **Rationale** |
| ----------------- | --------------------- | ------------- |
| _Nothing to show_ |

---

## 2. Back-end Test Specification: APIs

### 2.1. Locations of Back-end Tests and Instructions to Run Them

#### 2.1.1. Tests

| **Interface**                 | **Describe Group Location, No Mocks**                | **Describe Group Location, With Mocks**            | **Mocked Components**              |
| ----------------------------- | ---------------------------------------------------- | -------------------------------------------------- | ---------------------------------- |
| **POST /user**                | `tests/withoutMocks/routes/UserRoutes.test.ts#L47`   | `tests/withMocks/routes/UserRoutes.test.ts#L60`    | MongoDB, Firebase |
| **GET /user/:userID**         | `tests/withoutMocks/routes/UserRoutes.test.ts#L117`  | `tests/withMocks/routes/UserRoutes.test.ts#L168`   | MongoDB, Firebase |
| **POST /user/ban/:userID**    | `tests/withoutMocks/routes/UserRoutes.test.ts#L149`  | `tests/withMocks/routes/UserRoutes.test.ts#L230`   | MongoDB, Firebase |
| **PUT /fcm/:userID**          | `tests/withoutMocks/routes/NotificationRoutes.test.ts#L59` | `tests/withMocks/routes/NotificationRoutes.test.ts#L60` | MongoDB, Firebase |
| **GET /notifications/:userID**| `tests/withoutMocks/routes/NotificationRoutes.test.ts#L122` | `tests/withMocks/routes/NotificationRoutes.test.ts#L175` | MongoDB, Firebase |
| **POST /send-event/:userID**  | `tests/withoutMocks/routes/NotificationRoutes.test.ts#L166` | `tests/withMocks/routes/NotificationRoutes.test.ts#L290` | MongoDB, Firebase |
| **PUT /location/:userID**     | `tests/withoutMocks/routes/LocationRoutes.test.ts#L58` | `tests/withMocks/routes/LocationRoutes.test.ts#L57` | MongoDB, Firebase |
| **GET /friends/:userID**      | `tests/withoutMocks/routes/FriendRoutes.test.ts#L73` | `tests/withMocks/routes/FriendRoutes.test.ts#L62`  | MongoDB, Firebase |
| **POST /friends/:userID/sendRequest/:friendEmail** | `tests/withoutMocks/routes/FriendRoutes.test.ts#L113` | `tests/withMocks/routes/FriendRoutes.test.ts#L163` | MongoDB, Firebase |
| **GET /friends/:userID/friendRequests** | `tests/withoutMocks/routes/FriendRoutes.test.ts#L184` | `tests/withMocks/routes/FriendRoutes.test.ts#L359` | MongoDB, Firebase |
| **POST /friends/:userID/acceptRequest/:friendID** | `tests/withoutMocks/routes/FriendRoutes.test.ts#L229` | `tests/withMocks/routes/FriendRoutes.test.ts#L455` | MongoDB, Firebase |
| **POST /friends/:userID/declineRequest/:friendID** | `tests/withoutMocks/routes/FriendRoutes.test.ts#L341` | `tests/withMocks/routes/FriendRoutes.test.ts#L634` | MongoDB, Firebase |
| **PUT /friends/:userID/deleteFriend/:friendID** | `tests/withoutMocks/routes/FriendRoutes.test.ts#L294` | `tests/withMocks/routes/FriendRoutes.test.ts#L730` | MongoDB, Firebase |

#### 2.1.2. Commit Hash Where Tests Run

`[Insert Commit SHA here]`

#### 2.1.3. Explanation on How to Run the Tests

1. **Clone the Repository**:
   ```
   git clone [repository URL]
   cd CPEN321_CMIYC
   ```

2. **Install Dependencies**:
   ```
   cd Back_End
   npm install
   ```

3. **Environment Setup**:
   - Since we push our enviorment variables, you do not have to do this.
     

4. **Run Tests Without Mocks**:
   ```
   npm run test:nomocks
   ```
   This will run tests using an actual MongoDB instance. Make sure MongoDB is running locally.

5. **Run Tests With Mocks**:
   ```
   npm run test:mocks
   ```
   This will run tests with mocked MongoDB and Firebase dependencies.

6. **Run All Tests**:
   ```
   npm run test
   ```
   This will run both mocked and unmocked tests.

### 2.2. GitHub Actions Configuration Location

`.github/workflows/deploy.yml`

The GitHub Actions workflow is configured to run all tests in a CI environment before deployment. Key points:
- Tests run on push to the main branch
- MongoDB is set up as a service container for the tests
- Environment variables are configured from repository secrets
- Backend tests must pass before deployment proceeds to the EC2 instance

### 2.3. Jest Coverage Report Screenshots With Mocks

```peterscholtens@dhcp-206-12-70-97 Back_End % npm run test:mocks

> cmiyc@1.0.0 test:mocks
> jest tests/withMocks

 PASS  tests/withMocks/routes/FriendRoutes.test.ts
  FriendRoutes API - With Mocks
    GET /friends/:userID - Get Friends
      ✓ should return friends for user (33 ms)
      ✓ should return empty array for user with no friends (5 ms)
      ✓ should return 404 for non-existent user (3 ms)
    POST /friends/:userID/sendRequest/:friendEmail - Send Friend Request
      ✓ should send friend request successfully (6 ms)
      ✓ should return 400 when sending request to yourself (3 ms)
      ✓ should return 400 when already friends (3 ms)
      ✓ should return 200 when request already sent (5 ms)
      ✓ should return 404 for non-existent friend (4 ms)
    GET /friends/:userID/friendRequests - Get Friend Requests
      ✓ should return friend requests for user (6 ms)
      ✓ should return empty array for user with no friend requests (3 ms)
      ✓ should return 404 for non-existent user (4 ms)
    POST /friends/:userID/acceptRequest/:friendID - Accept Friend Request
      ✓ should accept friend request successfully (3 ms)
      ✓ should return 400 for non-existent friend request (3 ms)
      ✓ should return 404 for non-existent user or friend (4 ms)
      ✓ should handle database error when accepting friend request (3 ms)
    POST /friends/:userID/declineRequest/:friendID - Decline Friend Request
      ✓ should decline friend request successfully (3 ms)
      ✓ should return 404 for non-existent user (3 ms)
      ✓ should handle database error when declining friend request (3 ms)
    PUT /friends/:userID/deleteFriend/:friendID - Delete Friend
      ✓ should delete friend successfully (2 ms)
      ✓ should return 404 for non-existent user or friend (3 ms)
      ✓ should handle database error when deleting friend (3 ms)

 PASS  tests/withMocks/routes/NotificationRoutes.test.ts
  NotificationRoutes API - With Mocks
    PUT /fcm/:userID - Set FCM Token
      ✓ should update FCM token for existing user (8 ms)
      ✓ should return 404 for non-existent user (5 ms)
      ✓ should return 404 if update fails (3 ms)
      ✓ should return validation errors for missing FCM token (6 ms)
    GET /notifications/:userID - Get Notifications
      ✓ should return notifications for existing user (3 ms)
      ✓ should return 404 for non-existent user (5 ms)
      ✓ should handle user without logList property (2 ms)
      ✓ should return empty array for user with empty logList (3 ms)
    POST /send-event/:userID - Send Event Notification
      ✓ should send notification to nearby friends (5 ms)
      ✓ should return 404 for non-existent user (4 ms)
      ✓ should return 400 if user location not set (3 ms)
      ✓ should return validation errors for missing event name (4 ms)
      ✓ should handle Firebase messaging error (4 ms)

 PASS  tests/withMocks/routes/LocationRoutes.test.ts
  LocationRoutes API - With Mocks
    PUT /location/:userID - Update User Location
      ✓ should update location for existing user (6 ms)
      ✓ should return 404 for non-existent user (4 ms)
      ✓ should return validation errors for missing location data (3 ms)
      ✓ should return validation errors for invalid location format (3 ms)

 PASS  tests/withMocks/routes/UserRoutes.test.ts
  UserRoutes API - With Mocks
    POST /user - Create User Profile
      ✓ should create a new user profile when user doesnt exist (8 ms)
      ✓ should return existing user when user already exists (6 ms)
      ✓ should return banned status for banned user (3 ms)
    GET /user/:userID - Get All Users
      ✓ should return all users except the requesting user (3 ms)
    POST /user/ban/:userID - Ban User
      ✓ should ban a user (3 ms)
      ✓ should return 404 for non-existent user (3 ms)

 PASS  tests/withMocks/controllers/NotificationController.test.ts
  NotificationController - Quadtree Implementation
    findNearbyFriendsWithQuadtree
      ✓ should initialize quadtree with world boundary
      ✓ should insert friends with location into quadtree (1 ms)
      ✓ should calculate search range and query quadtree
      ✓ should return nearby friends from query results (1 ms)
      ✓ should correctly map and return points data as nearby friends (line 136)

 PASS  tests/withMocks/controllers/NotificationQuadtreeIntegration.test.ts
  NotificationController - Integration with Quadtree
    ✓ should call findNearbyFriendsWithQuadtree with user and friends (1 ms)
    ✓ should handle error when user not found
    ✓ should handle error when user location not set (1 ms)
    ✓ should update logList and send FCM notifications for nearby friends (1 ms)
    ✓ should handle friends without FCM tokens (line 136)

----------------------------|---------|----------|---------|---------|-------------------
File                        | % Stmts | % Branch | % Funcs | % Lines | Uncovered Line #s 
----------------------------|---------|----------|---------|---------|-------------------
All files                   |     100 |      100 |     100 |     100 |                   
 controllers                |     100 |      100 |     100 |     100 |                   
  FriendController.ts       |     100 |      100 |     100 |     100 |                   
  LocationController.ts     |     100 |      100 |     100 |     100 |                   
  NotificationController.ts |     100 |      100 |     100 |     100 |                   
  UserController.ts         |     100 |      100 |     100 |     100 |                   
 routes                     |     100 |      100 |     100 |     100 |                   
  FriendRoutes.ts           |     100 |      100 |     100 |     100 |                   
  LocationRoutes.ts         |     100 |      100 |     100 |     100 |                   
  NotificationRoutes.ts     |     100 |      100 |     100 |     100 |                   
  UserRoutes.ts             |     100 |      100 |     100 |     100 |                   
----------------------------|---------|----------|---------|---------|-------------------
Test Suites: 6 passed, 6 total
Tests:       54 passed, 54 total
Snapshots:   0 total
Time:        1.365 s
Ran all test suites matching /tests\/withMocks/i.
```

### 2.4. Jest Coverage Report Screenshots Without Mocks

```peterscholtens@dhcp-206-12-70-97 Back_End % npm run test:nomocks

> cmiyc@1.0.0 test:nomocks
> jest tests/withoutMocks

 PASS  tests/withoutMocks/routes/FriendErrorCoverage.test.ts
  FriendController Error Path Coverage
    acceptFriendRequest error handling (line 115)
      ✓ should handle errors in the catch block when database throws error (29 ms)
    declineFriendRequest error handling (line 136)
      ✓ Note: Line 136 is difficult to test directly in a non-mocked environment (1 ms)
    deleteFriend error handling (line 159)
      ✓ should handle errors in the catch block when database throws error (9 ms)

 PASS  tests/withoutMocks/routes/PerformanceTests.test.ts
  Performance Tests - Non-functional Requirements
    ✓ updateUserLocation should respond in under 0.5 seconds on average (66 ms)
    ✓ getFriends should respond in under 0.5 seconds on average (34 ms)
    ✓ getNotifications should respond in under 0.5 seconds on average (31 ms)

 PASS  tests/withoutMocks/routes/FriendRoutes.test.ts
  FriendRoutes API - No Mocks
    GET /friends/:userID - Get Friends
      ✓ should return empty array for user with no friends (5 ms)
      ✓ should return friend info for user with friends (covers lines 12-16) (6 ms)
      ✓ should return 404 for non-existent user (3 ms)
    POST /friends/:userID/sendRequest/:friendEmail - Send Friend Request
      ✓ should send friend request successfully (9 ms)
      ✓ should return 400 when sending request to yourself (5 ms)
      ✓ should return 400 when already friends (6 ms)
      ✓ should return 200 when request already sent (7 ms)
      ✓ should return 404 for non-existent friend (4 ms)
    GET /friends/:userID/friendRequests - Get Friend Requests
      ✓ should return friend requests for user (8 ms)
      ✓ should return 404 for non-existent user (3 ms)
    POST /friends/:userID/acceptRequest/:friendID - Accept Friend Request
      ✓ should accept friend request successfully (11 ms)
      ✓ should return 400 for non-existent friend request (4 ms)
      ✓ should return 404 for non-existent user (4 ms)
      ✓ should handle database errors gracefully in acceptFriendRequest (4 ms)
    PUT /friends/:userID/deleteFriend/:friendID - Delete Friend
      ✓ should delete friend successfully (9 ms)
      ✓ should return 404 for non-existent user or friend (5 ms)
      ✓ should handle database errors gracefully in deleteFriend (5 ms)
    POST /friends/:userID/declineRequest/:friendID - Decline Friend Request
      ✓ should decline friend request successfully (7 ms)
      ✓ should return 404 for non-existent user (3 ms)
      ✓ should handle database errors gracefully in declineFriendRequest (5 ms)

 PASS  tests/withoutMocks/routes/NotificationRoutes.test.ts
  NotificationRoutes API - No Mocks
    PUT /fcm/:userID - Update FCM Token
      ✓ should update FCM token successfully (9 ms)
      ✓ should return 404 for non-existent user (4 ms)
      ✓ should return 400 when FCM token is missing (3 ms)
    GET /notifications/:userID - Get Notifications
      ✓ should return notification log for user (5 ms)
      ✓ should return 404 for non-existent user (3 ms)
    POST /send-event/:userID - Send Event Notification
      ✓ should send event notification to nearby friends (9 ms)
      ✓ should return 404 for non-existent user (6 ms)
      ✓ should return 400 when user has no location set (6 ms)
      ✓ should handle errors gracefully in sendEventNotification (8 ms)

 PASS  tests/withoutMocks/routes/UserRoutes.test.ts
  UserRoutes API - No Mocks
    POST /user - Create User Profile
      ✓ should create a new user profile (9 ms)
      ✓ should handle banned user appropriately (4 ms)
      ✓ should return existing user profile if userID exists (7 ms)
      ✓ should return validation errors for missing fields (3 ms)
    GET /user/:userID - Get All Users
      ✓ should return all users except the requesting user (7 ms)
    POST /user/ban/:userID - Ban User
      ✓ should ban a user successfully (8 ms)
      ✓ should return 404 for non-existent user (4 ms)

 PASS  tests/withoutMocks/routes/LocationRoutes.test.ts
  LocationRoutes API - No Mocks
    PUT /location/:userID - Update User Location
      ✓ should update location for existing user (7 ms)
      ✓ should return 404 for non-existent user (3 ms)
      ✓ should return validation errors for missing location data (3 ms)
      ✓ should return validation errors for invalid location format (2 ms)

 PASS  tests/withoutMocks/routes/NotificationErrorCoverage.test.ts
  NotificationController Error Path Coverage
    ✓ Line 25 in NotificationController is difficult to cover directly
    sendEventNotification error handling (line 152)
      ✓ Error handling in sendEventNotification is challenging in non-mocked tests

----------------------------|---------|----------|---------|---------|-------------------
File                        | % Stmts | % Branch | % Funcs | % Lines | Uncovered Line #s 
----------------------------|---------|----------|---------|---------|-------------------
All files                   |   96.13 |    88.57 |     100 |   97.14 |                   
 controllers                |   95.78 |    88.57 |     100 |   96.87 |                   
  FriendController.ts       |   95.94 |      100 |     100 |   95.83 | 115,136,159       
  LocationController.ts     |     100 |      100 |     100 |     100 |                   
  NotificationController.ts |   92.59 |    55.55 |     100 |   96.07 | 25,152            
  UserController.ts         |     100 |      100 |     100 |     100 |                   
 routes                     |     100 |      100 |     100 |     100 |                   
  FriendRoutes.ts           |     100 |      100 |     100 |     100 |                   
  LocationRoutes.ts         |     100 |      100 |     100 |     100 |                   
  NotificationRoutes.ts     |     100 |      100 |     100 |     100 |                   
  UserRoutes.ts             |     100 |      100 |     100 |     100 |                   
----------------------------|---------|----------|---------|---------|-------------------
Test Suites: 7 passed, 7 total
Tests:       48 passed, 48 total
Snapshots:   0 total
Time:        1.728 s
Ran all test suites matching /tests\/withoutMocks/i.
```
## Justification for Lines with No Coverage

### NotificationController.ts (lines 25, 152)
- **Line 25**: The conditional block in `updateFCMToken` when `result.matchedCount === 0`
- **Line 152**: The catch block in `sendEventNotification`

#### Why these are challenging to cover:
- **Result Conditioning**: For line 25, we would need to have a scenario where a user exists (to pass initial validation) but then somehow doesn't match the update criteria. This is nearly impossible in a real database scenario without race conditions.
- **External Dependencies**: Line 152 involves Firebase messaging, which is an external service. Forcing it to throw errors in a controlled way is challenging without mocks.

### FriendController.ts (lines 115, 136, 159)
These are the error catch blocks in the following methods:
- **Line 115**: Catch block in the `acceptFriendRequest` method
- **Line 136**: Catch block in the `declineFriendRequest` method
- **Line 159**: Catch block in the `deleteFriend` method

#### Why these are challenging to cover:
- **Database Error Simulation**: In a non-mocked environment, it's difficult to force the database to throw errors at specific points in the code. When we tried to use invalid IDs, the MongoDB driver handled them by returning null rather than throwing exceptions.
- **Error Path Interception**: When using Express, errors are often caught at the middleware level before they reach the specific catch blocks in the controllers.
- **Race Conditions**: These error paths would typically be executed in real-world situations like database connection failures, timeouts, or when concurrent operations cause data inconsistencies.

### These lines are covered in Mocked Tests as it is the best approach to test because I can explicitly mock the database methods to throw exceptions.


---

## 3. App Test Specification: Tests of Non-Functional Requirements

### 3.1. Test Locations in Git

| **Non-Functional Requirement** | **Location in Git**                                                                  |
| ------------------------------ | ------------------------------------------------------------------------------------ |
| **App Startup Time**           | [`Front_End\app\src\androidTest\java\com\example\cmiyc\test\NFR1_StartupTime.kt`](#) |
| **API Response Time**          | [`Back_End/tests/withoutMocks/routes/PerformanceTests.test.ts`](#)                   |

### 3.2. Test Verification and Logs

- **App Startup Time**

  - **Verification:** The enhanced startup time tests now include comprehensive verification steps for cold, warm, and hot starts, ensuring accurate measurement and validation of app performance against Android Vitals benchmarks. For cold starts, the app is launched from a clean state (force-stopped and data-cleared) to verify the login screen appears within 5 seconds. Warm starts simulate a logged-in user returning after a backgrounded app kill, measuring time until the main UI (e.g., broadcast_button) loads under 2 seconds. Hot starts replicate real-world resumption via Recent Apps navigation, validating foreground restoration under 1.5 seconds. Each test incorporates explicit state management (e.g., permission grants, login flows), UI verification (checking critical elements like login_button or profile_button), and reliable timing using ActivityScenario and UiDevice interactions. Logging and assertions ensure thresholds are enforced, while cleanup steps (sign-out, process termination) maintain test isolation. The updates align with Android testing best practices, focusing on real user interactions and system behavior for accurate performance validation.
  - **Log Output**
    ```text
    2025-03-12 15:43:45.604 16222-16289 TestRunner              com.example.cmiyc                    I  started: test1ColdStartTime(com.example.cmiyc.test.NFR1_StartupTime)
    2025-03-12 15:43:46.632 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test setup started
    2025-03-12 15:43:46.643 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test setup completed
    2025-03-12 15:43:46.643 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  test1ColdStartTime Start
    2025-03-12 15:43:46.643 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  StartTime -------
    2025-03-12 15:43:46.746 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Duration: 103
    2025-03-12 15:43:46.746 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  test1ColdStartTime End
    2025-03-12 15:43:46.746 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test teardown started
    2025-03-12 15:43:46.747 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Cleared current user
    2025-03-12 15:43:46.752 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  WiFi and data enabled
    2025-03-12 15:43:46.775 16222-16222 NFR1_StartupTime        com.example.cmiyc                    D  Signed out from Google
    2025-03-12 15:43:46.775 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test teardown completed
    2025-03-12 15:43:47.083 16222-16289 TestRunner              com.example.cmiyc                    I  finished: test1ColdStartTime(com.example.cmiyc.test.NFR1_StartupTime)
    2025-03-12 15:43:47.543 16222-16289 TestRunner              com.example.cmiyc                    I  started: test2WarmStartTime(com.example.cmiyc.test.NFR1_StartupTime)
    2025-03-12 15:43:47.914 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test setup started
    2025-03-12 15:43:47.926 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test setup completed
    2025-03-12 15:43:47.926 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  test2WarmStartTime Start
    2025-03-12 15:43:47.926 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  setup for warm start
    2025-03-12 15:43:47.929 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Kill the app process
    2025-03-12 15:43:47.930 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Relaunch and measure warm start time
    2025-03-12 15:43:47.933 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Duration: 3
    2025-03-12 15:43:47.933 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  test2WarmStartTime End
    2025-03-12 15:43:47.933 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test teardown started
    2025-03-12 15:43:47.933 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Cleared current user
    2025-03-12 15:43:47.934 16222-16222 NFR1_StartupTime        com.example.cmiyc                    D  Signed out from Google
    2025-03-12 15:43:47.936 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  WiFi and data enabled
    2025-03-12 15:43:47.936 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test teardown completed
    2025-03-12 15:43:48.102 16222-16289 TestRunner              com.example.cmiyc                    I  finished: test2WarmStartTime(com.example.cmiyc.test.NFR1_StartupTime)
    2025-03-12 15:43:48.729 16222-16289 TestRunner              com.example.cmiyc                    I  started: test3HotStartTime(com.example.cmiyc.test.NFR1_StartupTime)
    2025-03-12 15:43:49.099 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test setup started
    2025-03-12 15:43:49.112 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test setup completed
    2025-03-12 15:43:49.112 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  test3HotStartTime Start
    2025-03-12 15:43:49.112 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  setup for Hot start
    2025-03-12 15:43:49.115 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Send the app to the background by pressing the Home button
    2025-03-12 15:43:50.883 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Re-start the app from background
    2025-03-12 15:43:51.483 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Duration: 1
    2025-03-12 15:43:51.484 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  test3HotStartTime End
    2025-03-12 15:43:51.484 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test teardown started
    2025-03-12 15:43:51.484 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Cleared current user
    2025-03-12 15:43:51.486 16222-16222 NFR1_StartupTime        com.example.cmiyc                    D  Signed out from Google
    2025-03-12 15:43:51.487 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  WiFi and data enabled
    2025-03-12 15:43:51.487 16222-16289 NFR1_StartupTime        com.example.cmiyc                    D  Test teardown completed
    2025-03-12 15:43:51.645 16222-16289 TestRunner              com.example.cmiyc                    I  finished: test3HotStartTime(com.example.cmiyc.test.NFR1_StartupTime)
    ```

- **API Response Time**
  - **Verification:** Performance tests validate that critical API endpoints respond within the 0.5 second (500ms) threshold required for smooth user experience. The tests run 10 iterations for each endpoint and calculate the average response time. Three key endpoints are tested: `updateUserLocation`, `getFriends`, and `getNotifications`. Each test executes repeated API calls with randomized data where appropriate (such as location coordinates) to simulate real-world usage patterns. Response times are measured by recording timestamps before and after each API call, with the final average calculated across all iterations. These tests run against an actual database connection rather than mocks to provide realistic performance metrics. This approach ensures that database operations, data processing, and API response generation all meet the performance requirements needed for the app's real-time tracking and social features.
  - **Log Output:** Average response times consistently fall below the 500ms threshold across all tested endpoints:
      > cmiyc@1.0.0 test
      > jest tests/withoutMocks/routes/PerformanceTests.test.ts
      
        console.log
          Average response time for updateUserLocation: 7.3 ms
      
            at tests/withoutMocks/routes/PerformanceTests.test.ts:83:13
      
        console.log
          Average response time for getFriends: 3.5 ms
      
            at tests/withoutMocks/routes/PerformanceTests.test.ts:107:13
      
        console.log
          Average response time for getNotifications: 3.3 ms
      
            at tests/withoutMocks/routes/PerformanceTests.test.ts:131:13
      
       PASS  tests/withoutMocks/routes/PerformanceTests.test.ts
        Performance Tests - Non-functional Requirements
          ✓ updateUserLocation should respond in under 0.5 seconds on average (80 ms)
          ✓ getFriends should respond in under 0.5 seconds on average (35 ms)
          ✓ getNotifications should respond in under 0.5 seconds on average (33 ms)

---

## 4. Front-end Test Specification

### 4.1. Location in Git of Front-end Test Suite:

`frontend/src/androidTest/java/com/studygroupfinder/`

### 4.2. Tests

- **Use Case: FR1_1 - Google Login (Success)**

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps (Derived from Code)** |
    | ------------------ | ----------------------------------------- |
    | 1. New and existing users click on the Google login button on the App's login page. | - Handle the location permission dialog (wait for and click the "Allow" button if it appears).<br>- Wait until the UI element with tag `login_button` exists.<br>- Click the UI element with tag `login_button`. |
    | 2. User is redirected to a page view where the user enters their Google email and password. | - The app displays the Google account picker. The test waits for a dialog containing the text "Choose an account".<br>- Simulate account selection by clicking on the account matching the test email (via `selectGoogleAccount(testAccountEmail)`). |
    | 3. Google Authentication succeeds. | - After selecting the account, the test waits until the main screen loads by checking for the UI element with tag `broadcast_button`, which indicates successful authentication. |
    | 4. User is redirected out of the login page and into the app's main page (Map/Main Screen). | - The appearance of the `broadcast_button` confirms the app has navigated to the main screen. Additional main-screen interactions (if any) would be handled here. |

  - **Test Logs:**
    ```
    2025-03-11 15:53:34.876 10000-10048 TestRunner              com.example.cmiyc                    I  started: test1Login(com.example.cmiyc.test.FR1_LoginWithGoogleAuthentication)
    2025-03-11 15:53:36.082 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test setup started
    2025-03-11 15:53:36.142 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test setup completed
    2025-03-11 15:53:36.142 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Starting test1Login
    2025-03-11 15:53:36.142 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Login process started
    2025-03-11 15:53:39.234 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Location permission dialog displayed
    2025-03-11 15:53:50.296 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Location permission dialog not found
    2025-03-11 15:53:50.302 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Login button exists, performing click
    2025-03-11 15:53:51.990 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Account picker displayed
    2025-03-11 15:53:55.135 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Selected account: guanhua.qiao2020@gmail.com
    2025-03-11 15:53:57.615 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Broadcast button exists after login
    2025-03-11 15:53:57.618 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Login process completed
    2025-03-11 15:53:57.618 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  test1Login completed
    2025-03-11 15:53:57.621 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test teardown started
    2025-03-11 15:53:57.622 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Cleared current user
    2025-03-11 15:53:57.649 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  WiFi and data enabled
    2025-03-11 15:53:57.669 10000-10000 FR1_LoginW...entication com.example.cmiyc                    D  Signed out from Google
    2025-03-11 15:53:57.687 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test teardown completed
    2025-03-11 15:53:58.181 10000-10048 TestRunner              com.example.cmiyc                    I  finished: test1Login(com.example.cmiyc.test.FR1_LoginWithGoogleAuthentication)
    ```

- **Use Case: FR1_1 - Google Login (Failure)**

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps (Derived from Code)** |
    | ------------------ | ----------------------------------------- |
    | 1. New and existing users click on the Google login button on the App's login page. | - Disable network connectivity by executing shell commands to turn off WiFi and mobile data (simulating a network error).<br>- Handle the location permission dialog (wait for and click the "Allow" button if it appears).<br>- Wait until the UI element with tag `login_button` exists and click it. |
    | 2. User is redirected to a page view where the user enters their Google email and password. | - The Google account picker is displayed. The test attempts to select the test account, which, due to the disabled network, causes the authentication process to fail. |
    | 2a. Network error while contacting Google Auth services. | - The network failure triggers the display of a "Login Error" dialog.<br>- Wait until a UI element with the text `Login Error` appears, confirming the error condition. |
    | 2a1. Display the following: "Login Error: Network Error during Registration". | - Assert that the error dialog containing `Login Error` is displayed (as verified by the presence of the text on screen). |
    | 2a2. When the OK button is clicked, user is redirected back to the login page. | - Click on the button labeled `OK` in the error dialog.<br>- Wait until the UI element with tag `login_button` reappears, confirming that the app navigated back to the login page. |

  - **Test Logs:**
    ```
    2025-03-11 15:54:19.212 10000-10048 TestRunner              com.example.cmiyc                    I  started: test3LoginFailure(com.example.cmiyc.test.FR1_LoginWithGoogleAuthentication)
    2025-03-11 15:54:19.573 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test setup started
    2025-03-11 15:54:19.594 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test setup completed
    2025-03-11 15:54:19.594 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Starting test3LoginFailure
    2025-03-11 15:54:19.596 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  WiFi and data disabled
    2025-03-11 15:54:22.628 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Location permission dialog displayed
    2025-03-11 15:54:33.669 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Location permission dialog not found
    2025-03-11 15:54:33.681 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Clicked on login button
    2025-03-11 15:54:34.719 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Account picker displayed
    2025-03-11 15:54:37.861 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Selected account: guanhua.qiao2020@gmail.com
    2025-03-11 15:54:38.060 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Login Error dialog exists
    2025-03-11 15:54:38.068 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Clicked OK on Login Error dialog
    2025-03-11 15:54:38.123 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Login button exists after error
    2025-03-11 15:54:38.125 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  test3LoginFailure completed
    2025-03-11 15:54:38.125 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test teardown started
    2025-03-11 15:54:38.125 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Cleared current user
    2025-03-11 15:54:38.127 10000-10000 FR1_LoginW...entication com.example.cmiyc                    D  Signed out from Google
    2025-03-11 15:54:38.129 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  WiFi and data enabled
    2025-03-11 15:54:38.129 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test teardown completed
    2025-03-11 15:54:38.321 10000-10048 TestRunner              com.example.cmiyc                    I  finished: test3LoginFailure(com.example.cmiyc.test.FR1_LoginWithGoogleAuthentication)
    ```

- **Use Case: FR1_2 - Logout**

  - **Expected Behaviors:**
    | Scenario Steps | Test Case Steps (Derived from Code) |
    | ----------------------------------------------------- | ----------------------------------------- |
    | 1. From a profile page, the user clicks logout. | - First, perform a successful login (following the FR1_1 – Google Login Success steps).<br>- On the main screen, click the UI element with tag `profile_button` to navigate to the profile page. |
    | 2. User is redirected to the login page. | - On the profile page, wait until the UI element with tag `signout_button` appears and then click it.<br>- Wait until the UI element with tag `login_button` is present, confirming that the app has navigated back to the login page. |

  - **Test Logs:**
    ```
    2025-03-11 15:53:58.822 10000-10048 TestRunner              com.example.cmiyc                    I  started: test2Signout(com.example.cmiyc.test.FR1_LoginWithGoogleAuthentication)
    2025-03-11 15:53:59.238 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test setup started
    2025-03-11 15:53:59.263 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test setup completed
    2025-03-11 15:53:59.263 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Starting test2Signout
    2025-03-11 15:53:59.263 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Login process started
    2025-03-11 15:54:02.309 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Location permission dialog displayed
    2025-03-11 15:54:13.370 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Location permission dialog not found
    2025-03-11 15:54:13.410 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Login button exists, performing click
    2025-03-11 15:54:14.961 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Account picker displayed
    2025-03-11 15:54:17.701 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Selected account: guanhua.qiao2020@gmail.com
    2025-03-11 15:54:18.131 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Broadcast button exists after login
    2025-03-11 15:54:18.133 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Login process completed
    2025-03-11 15:54:18.142 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Clicked on profile button
    2025-03-11 15:54:18.425 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Signout button exists, performing click
    2025-03-11 15:54:18.525 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Login button exists after signout
    2025-03-11 15:54:18.527 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  test2Signout completed
    2025-03-11 15:54:18.527 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test teardown started
    2025-03-11 15:54:18.527 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Cleared current user
    2025-03-11 15:54:18.529 10000-10000 FR1_LoginW...entication com.example.cmiyc                    D  Signed out from Google
    2025-03-11 15:54:18.536 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  WiFi and data enabled
    2025-03-11 15:54:18.537 10000-10048 FR1_LoginW...entication com.example.cmiyc                    D  Test teardown completed
    2025-03-11 15:54:18.698 10000-10048 TestRunner              com.example.cmiyc                    I  finished: test2Signout(com.example.cmiyc.test.FR1_LoginWithGoogleAuthentication)
    ```

- **Use Case: FR2_1 - Add Friend (Success)**

  - **Expected Behaviors:**

    | **Scenario Steps**                                          | **Test Case Steps**                                                                                                                                                             |
    | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
    | 1. User clicks the friends button to access the friend page | - Login with test account `guanhua.qiao2020@gmail.com` (using `login(testAccountEmail1)`).<br>- Wait until the UI element with tag `friends_button` exists and perform a click. |
    | 2. User clicks the add friend button                        | - Wait until the UI element with tag `addFriends_button` exists and perform a click to open the add friend dialog.                                                              |
    | 3. User enters friend's email address in dialog box         | - Wait until the UI element with tag `friendEmail_Input` exists and perform text input with email `omiduckai@gmail.com`.                                                        |
    | 4. User clicks Send Request                                 | - Click the UI element with tag `submitFriendEmail_button`.<br>- Verify that `friendEmail_Input` no longer exists, confirming the request was sent.                             |

  - **Test Logs:**
    ```
    2025-03-11 16:14:20.603 15823-15870 TestRunner              com.example.cmiyc                    I  started: test1AddFriend(com.example.cmiyc.test.FR2_ManageFriends)
    2025-03-11 16:14:21.939 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Test setup started
    2025-03-11 16:14:21.989 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission granted
    2025-03-11 16:14:22.017 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  WiFi and data enabled
    2025-03-11 16:14:22.017 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting test1AddFriend
    2025-03-11 16:14:22.017 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting login process for email: guanhua.qiao2020@gmail.com
    2025-03-11 16:14:22.017 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Handling location permission dialog
    2025-03-11 16:14:25.072 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog found
    2025-03-11 16:14:36.111 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog not found or already granted
    2025-03-11 16:14:36.121 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Login button found
    2025-03-11 16:14:36.174 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked login button
    2025-03-11 16:14:36.174 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selecting Google account: guanhua.qiao2020@gmail.com
    2025-03-11 16:14:37.350 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Account picker dialog found
    2025-03-11 16:14:37.730 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selected Google account: guanhua.qiao2020@gmail.com
    2025-03-11 16:14:39.874 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Broadcast button found, login successful
    2025-03-11 16:14:39.902 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked on friends button
    2025-03-11 16:14:39.902 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Friends screen opened
    2025-03-11 16:14:40.628 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked on add friends button
    2025-03-11 16:14:40.628 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Add Friend button clicked
    2025-03-11 16:14:41.478 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Entered friend email: omiduckai@gmail.com
    2025-03-11 16:14:41.478 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Friend email input entered: omiduckai@gmail.com
    2025-03-11 16:14:41.620 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked submit friend email button
    2025-03-11 16:14:41.868 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Friend email submitted and input field removed
    2025-03-11 16:14:41.885 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting tearDown
    2025-03-11 16:14:41.910 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Cleared current user
    2025-03-11 16:14:41.967 15823-15823 FR2_ManageFriends       com.example.cmiyc                    D  Signed out from Google account
    2025-03-11 16:14:41.968 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Intents released
    2025-03-11 16:14:42.882 15823-15870 TestRunner              com.example.cmiyc                    I  finished: test1AddFriend(com.example.cmiyc.test.FR2_ManageFriends)
    ```

- **Use Case: FR2_1 - Add Friend (Failure)**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                                  | **Test Case Steps**                                                                                                                                                                                                                                 |
    | ----------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
    | 1. User clicks the friends button to access the friend page                         | - Login with test account `omiduckai@gmail.com` (using `login(testAccountEmail2)`).<br>- Wait until the UI element with tag `friends_button` exists and perform a click.                                                                            |
    | 2. User clicks the add friend button                                                | - Wait until the UI element with tag `addFriends_button` exists and perform a click.                                                                                                                                                                |
    | 3. User enters friend's email address in dialog box                                 | - For branch 4b: Enter an invalid email (`invalidEmail@invalid.com`) in the UI element with tag `friendEmail_Input`.<br>- For branch 4a: (later) re-enter a valid email after disabling network.                                                    |
    | 4. User clicks Send Request                                                         | - Click the UI element with tag `submitFriendEmail_button`.<br>- Wait until an error dialog with text `Error` appears.                                                                                                                              |
    | 4b. Friend has never registered in the app                                          | - Error is triggered by entering an invalid email.                                                                                                                                                                                                  |
    | 4b1. Display "Failed to send friend request: 404"                                   | - Verify the error dialog displays "Error" (interpreting it as a 404 error).                                                                                                                                                                        |
    | 4b2. User clicks the only option "ok"                                               | - Click the button with text `OK` on the error dialog.                                                                                                                                                                                              |
    | 4a. Sending friend request fails due to a network error.                            | - Disable network connectivity by executing shell commands to turn off WiFi and mobile data.<br>- Wait until the UI element `friendEmail_Input` reappears, then enter a valid email (`omiduckai@gmail.com`).<br>- Click `submitFriendEmail_button`. |
    | 4a1. Display the error message: "Error: Network error while sending friend request" | - Verify that an error dialog with text `Error` is displayed.                                                                                                                                                                                       |
    | 4a2. User clicks the only option "ok"                                               | - Click the `OK` button on the error dialog.                                                                                                                                                                                                        |
    | 4a3. User redirected back to friend request dialog box                              | - Verify that the add friend dialog (i.e. `friendEmail_Input`) is present again for reattempt.                                                                                                                                                      |

  - **Test Logs:**
    ```
    2025-03-11 16:14:43.267 15823-15870 TestRunner              com.example.cmiyc                    I  started: test2AddFriendFailure(com.example.cmiyc.test.FR2_ManageFriends)
    2025-03-11 16:14:43.938 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Test setup started
    2025-03-11 16:14:44.006 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission granted
    2025-03-11 16:14:44.013 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  WiFi and data enabled
    2025-03-11 16:14:44.013 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting test2AddFriendFailure
    2025-03-11 16:14:44.013 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting login process for email: omiduckai@gmail.com
    2025-03-11 16:14:44.013 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Handling location permission dialog
    2025-03-11 16:14:47.114 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog found
    2025-03-11 16:14:58.153 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog not found or already granted
    2025-03-11 16:14:58.203 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Login button found
    2025-03-11 16:14:58.371 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked login button
    2025-03-11 16:14:58.371 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selecting Google account: omiduckai@gmail.com
    2025-03-11 16:14:59.719 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Account picker dialog found
    2025-03-11 16:15:00.072 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selected Google account: omiduckai@gmail.com
    2025-03-11 16:15:01.531 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Broadcast button found, login successful
    2025-03-11 16:15:01.559 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked on friends button
    2025-03-11 16:15:01.559 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Friends screen opened
    2025-03-11 16:15:01.792 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked on add friends button
    2025-03-11 16:15:01.792 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Attempting to add invalid email
    2025-03-11 16:15:02.259 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Entered invalid email: invalidEmail@invalid.com
    2025-03-11 16:15:02.341 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked submit friend email button
    2025-03-11 16:15:03.140 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Error dialog displayed for invalid email
    2025-03-11 16:15:03.166 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked OK on error dialog
    2025-03-11 16:15:03.184 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  WiFi and data disabled for network error simulation
    2025-03-11 16:15:03.565 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Entered friend email: omiduckai@gmail.com
    2025-03-11 16:15:03.656 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked submit friend email button
    2025-03-11 16:15:04.229 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Error dialog displayed for network error
    2025-03-11 16:15:04.230 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Network error handled, error dialog shown
    2025-03-11 16:15:04.235 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting tearDown
    2025-03-11 16:15:04.236 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Cleared current user
    2025-03-11 16:15:04.270 15823-15823 FR2_ManageFriends       com.example.cmiyc                    D  Signed out from Google account
    2025-03-11 16:15:04.272 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Intents released
    2025-03-11 16:15:04.890 15823-15870 TestRunner              com.example.cmiyc                    I  finished: test2AddFriendFailure(com.example.cmiyc.test.FR2_ManageFriends)
    ```

- **Use Case: FR2_2 - Respond to Friend Requests (Failure)**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                                                    | **Test Case Steps**                                                                                                                                                    |
    | ----------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
    | 1. User receives a friend request.                                                                    | - Ensure that at least one pending friend request exists by verifying the presence of a UI element with tag `acceptFriend_button`.                                     |
    | 2. User navigates to friends page                                                                     | - Login with test account `omiduckai@gmail.com`.<br>- Click the UI element with tag `friends_button`.                                                                  |
    | 3. User clicks the friend requests icon                                                               | - Click the UI element with tag `friendRequests_button` to display the friend requests.                                                                                |
    | 4. Friend requests to the User are displayed                                                          | - Verify that one or more UI elements with tag `acceptFriend_button` are present in the friend requests dialog.                                                        |
    | 5. If the user accepts, that friend is added to their friend list and the friend's list adds the user | - (Success branch not exercised here.)                                                                                                                                 |
    | 5a. Network error when accepting or declining friend requests.                                        | - Disable network connectivity by executing shell commands to turn off WiFi and mobile data.<br>- Click the first available UI element with tag `acceptFriend_button`. |
    | 5a1. Display the error message: "Network error while accepting/declining friend request"              | - Wait until an error dialog with text `Error` is displayed.                                                                                                           |
    | 5a2. User clicks the only option "ok"                                                                 | - Click the button with text `OK` on the error dialog.                                                                                                                 |
    | 5a3. User is redirected back to friend requests dialog box                                            | - Verify that the friend requests dialog remains accessible for retry.                                                                                                 |

  - **Test Logs:**

    ```
    2025-03-11 16:15:05.457 15823-15870 TestRunner              com.example.cmiyc                    I  started: test3RespondFriendRequestFailure(com.example.cmiyc.test.FR2_ManageFriends)
    2025-03-11 16:15:05.956 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Test setup started
    2025-03-11 16:15:06.002 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission granted
    2025-03-11 16:15:06.006 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  WiFi and data enabled
    2025-03-11 16:15:06.006 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting test3RespondFriendRequestFailure
    2025-03-11 16:15:06.006 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting login process for email: omiduckai@gmail.com
    2025-03-11 16:15:06.006 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Handling location permission dialog
    2025-03-11 16:15:09.033 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog found
    2025-03-11 16:15:20.231 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog not found or already granted
    2025-03-11 16:15:20.233 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Login button found
    2025-03-11 16:15:20.246 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked login button
    2025-03-11 16:15:20.246 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selecting Google account: omiduckai@gmail.com
    2025-03-11 16:15:21.723 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Account picker dialog found
    2025-03-11 16:15:24.875 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selected Google account: omiduckai@gmail.com
    2025-03-11 16:15:25.361 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Broadcast button found, login successful
    2025-03-11 16:15:25.381 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked on friends button
    2025-03-11 16:15:25.639 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked on friend requests button
    2025-03-11 16:15:26.607 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Found accept friend button
    2025-03-11 16:15:26.610 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  WiFi and data disabled for request failure simulation
    2025-03-11 16:15:29.631 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked accept friend button
    2025-03-11 16:15:29.930 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Error dialog displayed for friend request failure
    2025-03-11 16:15:29.931 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Error dialog shown for friend request failure
    2025-03-11 16:15:29.931 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting tearDown
    2025-03-11 16:15:29.932 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Cleared current user
    2025-03-11 16:15:29.933 15823-15823 FR2_ManageFriends       com.example.cmiyc                    D  Signed out from Google account
    2025-03-11 16:15:29.933 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Intents released
    2025-03-11 16:15:30.236 15823-15870 TestRunner              com.example.cmiyc                    I  finished: test3RespondFriendRequestFailure(com.example.cmiyc.test.FR2_ManageFriends)
    ```

- **Use Case: FR2_2 - Respond to Friend Requests (Success)**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                                                    | **Test Case Steps**                                                                                                                                           |
    | ----------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
    | 1. User receives a friend request.                                                                    | - Ensure that at least one pending friend request exists by verifying the presence of a UI element with tag `acceptFriend_button`.                            |
    | 2. User navigates to friends page                                                                     | - Login with test account `omiduckai@gmail.com`.<br>- Click the UI element with tag `friends_button`.                                                         |
    | 3. User clicks the friend requests icon                                                               | - Click the UI element with tag `friendRequests_button` to display friend requests.                                                                           |
    | 4. Friend requests to the User are displayed                                                          | - Verify that one or more UI elements with tag `acceptFriend_button` are present.                                                                             |
    | 5. If the user accepts, that friend is added to their friend list and the friend's list adds the user | - Click the first available UI element with tag `acceptFriend_button`.<br>- Verify that no error dialog appears, confirming the friend is added successfully. |
    | 6a. If the user declines, the friend request is cleared.                                              | - (Decline action is not explicitly implemented in the test code, but would clear the request if performed.)                                                  |

  - **Test Logs:**

    ```
    2025-03-11 16:15:30.753 15823-15870 TestRunner              com.example.cmiyc                    I  started: test4RespondFriendRequest(com.example.cmiyc.test.FR2_ManageFriends)
    2025-03-11 16:15:31.106 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Test setup started
    2025-03-11 16:15:31.118 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission granted
    2025-03-11 16:15:31.121 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  WiFi and data enabled
    2025-03-11 16:15:31.121 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting test4RespondFriendRequest
    2025-03-11 16:15:31.121 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting login process for email: omiduckai@gmail.com
    2025-03-11 16:15:31.121 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Handling location permission dialog
    2025-03-11 16:15:34.132 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog found
    2025-03-11 16:15:45.276 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog not found or already granted
    2025-03-11 16:15:45.278 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Login button found
    2025-03-11 16:15:45.290 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked login button
    2025-03-11 16:15:45.290 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selecting Google account: omiduckai@gmail.com
    2025-03-11 16:15:46.369 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Account picker dialog found
    2025-03-11 16:15:49.507 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selected Google account: omiduckai@gmail.com
    2025-03-11 16:15:49.731 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Broadcast button found, login successful
    2025-03-11 16:15:49.744 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked on friends button
    2025-03-11 16:15:49.856 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked on friend requests button
    2025-03-11 16:15:51.215 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked accept friend button
    2025-03-11 16:15:51.215 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Friend request accepted successfully
    2025-03-11 16:15:51.215 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting tearDown
    2025-03-11 16:15:51.215 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Cleared current user
    2025-03-11 16:15:51.225 15823-15823 FR2_ManageFriends       com.example.cmiyc                    D  Signed out from Google account
    2025-03-11 16:15:51.225 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Intents released
    2025-03-11 16:15:51.399 15823-15870 TestRunner              com.example.cmiyc                    I  finished: test4RespondFriendRequest(com.example.cmiyc.test.FR2_ManageFriends)
    ```

- **Use Case: FR2_3 - Remove Friend (Failure)**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                       | **Test Case Steps**                                                                                                                                                          |
    | ------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
    | 1. User navigates to friends page                                        | - Login with test account `omiduckai@gmail.com`.<br>- Wait until the UI element with tag `friends_button` exists and click it.                                               |
    | 2. User clicks on the button beside the friend to remove them.           | - Verify that at least one UI element with tag `removeFriend_button` exists (using onAllNodes with `removeFriend_button` and selecting the first one), then perform a click. |
    | 2a. Network error when removing friends.                                 | - Disable network connectivity by executing shell commands to turn off WiFi and mobile data.                                                                                 |
    | 2a1. Display the error message "Network error: failed to remove friend". | - Wait until an error dialog with text `Error` is displayed.                                                                                                                 |
    | 2a2. User clicks the only option "ok".                                   | - Click the button with text `OK` on the error dialog.                                                                                                                       |
    | 2a3. User is redirected back to friends page.                            | - Verify that the friends page (UI element `friends_button`) is visible again.                                                                                               |

  - **Test Logs:**

    ```
    2025-03-11 16:15:51.915 15823-15870 TestRunner              com.example.cmiyc                    I  started: test5RemoveFriendFailure(com.example.cmiyc.test.FR2_ManageFriends)
    2025-03-11 16:15:52.271 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Test setup started
    2025-03-11 16:15:52.288 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission granted
    2025-03-11 16:15:52.290 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  WiFi and data enabled
    2025-03-11 16:15:52.290 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting test5RemoveFriendFailure
    2025-03-11 16:15:52.290 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting login process for email: omiduckai@gmail.com
    2025-03-11 16:15:52.290 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Handling location permission dialog
    2025-03-11 16:15:55.296 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog found
    2025-03-11 16:16:06.639 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog not found or already granted
    2025-03-11 16:16:06.643 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Login button found
    2025-03-11 16:16:06.656 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked login button
    2025-03-11 16:16:06.657 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selecting Google account: omiduckai@gmail.com
    2025-03-11 16:16:07.728 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Account picker dialog found
    2025-03-11 16:16:10.857 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selected Google account: omiduckai@gmail.com
    2025-03-11 16:16:11.069 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Broadcast button found, login successful
    2025-03-11 16:16:11.081 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked on friends button
    2025-03-11 16:16:11.197 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Found remove friend button
    2025-03-11 16:16:11.199 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  WiFi and data disabled for remove friend failure simulation
    2025-03-11 16:16:16.237 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked remove friend button
    2025-03-11 16:16:16.401 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Error dialog displayed for remove friend failure
    2025-03-11 16:16:16.402 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Error dialog shown for remove friend failure
    2025-03-11 16:16:16.402 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting tearDown
    2025-03-11 16:16:16.402 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Cleared current user
    2025-03-11 16:16:16.407 15823-15823 FR2_ManageFriends       com.example.cmiyc                    D  Signed out from Google account
    2025-03-11 16:16:16.407 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Intents released
    2025-03-11 16:16:16.663 15823-15870 TestRunner              com.example.cmiyc                    I  finished: test5RemoveFriendFailure(com.example.cmiyc.test.FR2_ManageFriends)
    ```

- **Use Case: FR2_3 - Remove Friend (Success)**

  - **Expected Behaviors:**

    | **Scenario Steps**                                             | **Test Case Steps**                                                                                                                                                          |
    | -------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
    | 1. User navigates to friends page                              | - Login with test account `omiduckai@gmail.com`.<br>- Wait until the UI element with tag `friends_button` exists and click it.                                               |
    | 2. User clicks on the button beside the friend to remove them. | - Verify that at least one UI element with tag `removeFriend_button` exists (using onAllNodes with `removeFriend_button` and selecting the first one), then perform a click. |
    | 3. The friend is removed from the user's friend list.          | - Implicitly verify that no error dialog appears after the removal action, confirming the friend has been removed.                                                           |
    | 4. The user is also removed from the friend's friends list     | - (Success is assumed to update both lists; further UI verifications can be added as needed.)                                                                                |

  - **Test Logs:**

    ```
    2025-03-11 16:16:17.126 15823-15870 TestRunner              com.example.cmiyc                    I  started: test6RemoveFriendRequest(com.example.cmiyc.test.FR2_ManageFriends)
    2025-03-11 16:16:17.488 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Test setup started
    2025-03-11 16:16:17.500 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission granted
    2025-03-11 16:16:17.502 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  WiFi and data enabled
    2025-03-11 16:16:17.502 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting test6RemoveFriendRequest
    2025-03-11 16:16:17.502 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting login process for email: omiduckai@gmail.com
    2025-03-11 16:16:17.502 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Handling location permission dialog
    2025-03-11 16:16:20.603 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog found
    2025-03-11 16:16:31.635 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Location permission dialog not found or already granted
    2025-03-11 16:16:31.637 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Login button found
    2025-03-11 16:16:31.648 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked login button
    2025-03-11 16:16:31.648 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selecting Google account: omiduckai@gmail.com
    2025-03-11 16:16:32.818 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Account picker dialog found
    2025-03-11 16:16:35.957 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Selected Google account: omiduckai@gmail.com
    2025-03-11 16:16:36.166 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Broadcast button found, login successful
    2025-03-11 16:16:36.178 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked on friends button
    2025-03-11 16:16:36.297 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Clicked remove friend button
    2025-03-11 16:16:36.297 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Friend removed successfully
    2025-03-11 16:16:36.298 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Starting tearDown
    2025-03-11 16:16:36.298 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Cleared current user
    2025-03-11 16:16:36.307 15823-15823 FR2_ManageFriends       com.example.cmiyc                    D  Signed out from Google account
    2025-03-11 16:16:36.307 15823-15870 FR2_ManageFriends       com.example.cmiyc                    D  Intents released
    2025-03-11 16:16:36.493 15823-15870 TestRunner              com.example.cmiyc                    I  finished: test6RemoveFriendRequest(com.example.cmiyc.test.FR2_ManageFriends)
    ```

- **Use Case: FR5_1 - User views the activity logs (Success)**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                           | **Test Case Steps**                                                                                                                                                                |
    | ---------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
    | 1. The user switches to the "Activity Logs" page                             | - Login with test account `guanhua.qiao2020@gmail.com` (performed in setup).<br>- Click the UI element with tag `log_button` to navigate to the Activity Logs page.                |
    | 2. The user can scroll up and down the log to see all received activity logs | - Wait until a log entry (e.g., a UI element with text `Test`) appears within the Activity Logs, confirming that log entries are available and visible.                            |
    | 3. The logs are sorted from most recent to least recent                      | - Implicitly verify that the expected log entry (with text `Test`) is displayed, indicating that the logs are loaded (sorting is assumed by the display order of the log content). |

  - **Test Logs:**

    ```
    2025-03-11 16:21:21.777 19900-19939 TestRunner              com.example.cmiyc                    I  started: test1ViewLogs(com.example.cmiyc.test.FR5_ViewActivityLogs)
    2025-03-11 16:21:23.211 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Test setup started
    2025-03-11 16:21:23.278 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Location permission granted
    2025-03-11 16:21:23.282 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  WiFi and data enabled
    2025-03-11 16:21:23.282 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Starting login process
    2025-03-11 16:21:23.282 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Handling location permission dialog
    2025-03-11 16:21:24.516 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Location permission dialog found
    2025-03-11 16:21:25.337 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Location permission granted
    2025-03-11 16:21:25.530 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Login button found
    2025-03-11 16:21:25.681 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Clicked on login button
    2025-03-11 16:21:25.681 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Selecting Google account: guanhua.qiao2020@gmail.com
    2025-03-11 16:21:27.172 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Account picker dialog found
    2025-03-11 16:21:28.921 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Selected Google account: guanhua.qiao2020@gmail.com
    2025-03-11 16:21:31.007 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Broadcast button found, login successful
    2025-03-11 16:21:31.025 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Starting test1ViewLogs
    2025-03-11 16:21:31.075 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Clicked on log button
    2025-03-11 16:21:31.547 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Log entry 'Test' found
    2025-03-11 16:21:31.569 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Log entry 'Test' is displayed
    2025-03-11 16:21:31.571 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Starting tearDown
    2025-03-11 16:21:31.574 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Cleared current user
    2025-03-11 16:21:31.594 19900-19900 FR5_ViewActivityLogs    com.example.cmiyc                    D  Signed out from Google account
    2025-03-11 16:21:31.594 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Intents released
    2025-03-11 16:21:32.260 19900-19939 TestRunner              com.example.cmiyc                    I  finished: test1ViewLogs(com.example.cmiyc.test.FR5_ViewActivityLogs)
    ```

- **Use Case: FR5_1 - User views the activity logs (Failure)**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                             | **Test Case Steps**                                                                                                                                                 |
    | ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
    | 1. The user switches to the "Activity Logs" page                               | - Login with test account `guanhua.qiao2020@gmail.com` (performed in setup).<br>- Click the UI element with tag `log_button` to navigate to the Activity Logs page. |
    | 1a. Network error while fetching activity log                                  | - Execute shell commands to disable WiFi and mobile data, simulating a network error.<br>- Wait until the Activity Logs page displays an error message.             |
    | 1a1. Display the "Sync Problem" Alert to ask user to check internet connection | - Verify that a UI element with text `Sync Problem` exists, indicating that the app has detected a network problem while fetching the logs.                         |

  - **Test Logs:**

    ```
    2025-03-11 16:21:32.852 19900-19939 TestRunner              com.example.cmiyc                    I  started: test2ViewLogsFailure(com.example.cmiyc.test.FR5_ViewActivityLogs)
    2025-03-11 16:21:33.362 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Test setup started
    2025-03-11 16:21:33.448 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Location permission granted
    2025-03-11 16:21:33.453 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  WiFi and data enabled
    2025-03-11 16:21:33.454 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Starting login process
    2025-03-11 16:21:33.454 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Handling location permission dialog
    2025-03-11 16:21:36.549 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Location permission dialog found
    2025-03-11 16:21:47.595 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Location permission dialog not found or already granted
    2025-03-11 16:21:47.605 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Login button found
    2025-03-11 16:21:47.659 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Clicked on login button
    2025-03-11 16:21:47.659 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Selecting Google account: guanhua.qiao2020@gmail.com
    2025-03-11 16:21:49.068 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Account picker dialog found
    2025-03-11 16:21:52.232 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Selected Google account: guanhua.qiao2020@gmail.com
    2025-03-11 16:21:53.025 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Broadcast button found, login successful
    2025-03-11 16:21:53.031 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Starting test2ViewLogsFailure
    2025-03-11 16:21:53.043 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Clicked on log button
    2025-03-11 16:21:53.060 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  WiFi and data disabled for failure simulation
    2025-03-11 16:24:23.621 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Sync Problem message found
    2025-03-11 16:24:23.624 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Sync Problem message is displayed
    2025-03-11 16:24:23.627 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Starting tearDown
    2025-03-11 16:24:23.629 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Cleared current user
    2025-03-11 16:24:23.632 19900-19900 FR5_ViewActivityLogs    com.example.cmiyc                    D  Signed out from Google account
    2025-03-11 16:24:23.632 19900-19939 FR5_ViewActivityLogs    com.example.cmiyc                    D  Intents released
    2025-03-11 16:24:23.877 19900-19939 TestRunner              com.example.cmiyc                    I  finished: test2ViewLogsFailure(com.example.cmiyc.test.FR5_ViewActivityLogs)
    ```

---

## 5. Automated Code Review Results

### 5.1. Commit Hash Where Codacy Ran

`[Insert Commit SHA here]`

### 5.2. Unfixed Issues per Codacy Category

_(Placeholder for screenshots of Codacy's Category Breakdown table in Overview)_

### 5.3. Unfixed Issues per Codacy Code Pattern

_(Placeholder for screenshots of Codacy's Issues page)_

### 5.4. Justifications for Unfixed Issues

- **Code Pattern: [Usage of Deprecated Modules](#)**

  1. **Issue**

     - **Location in Git:** [`src/services/chatService.js#L31`](#)
     - **Justification:** ...

  2. ...

- ...
