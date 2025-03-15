# Backend Testing with Jest

This directory contains automated tests for the backend APIs using Jest.

## Directory Structure

- `withoutMocks/`: Tests that use real external components without mocking
- `withMocks/`: Tests that use mocked external components
- `testSetup.ts`: Common test setup utilities

## Running Tests

You can run tests using the npm scripts defined in `package.json`:

### Run all tests

```bash
npm test
```

### Run only tests with mocks

```bash
npm run test:mocks
```

### Run only tests without mocks

```bash
npm run test:nomocks
```

### Run tests with coverage information

```bash
npm run test:coverage
```

The coverage report will be generated in the `coverage` directory.

## Test Organization

Each test file follows this structure:

1. For each API endpoint, there are two main `describe` groups:
   - Tests without mocking
   - Tests with mocking

2. Each test is annotated with:
   - Information about inputs
   - Expected returned status code
   - Expected outputs
   - Expected behavior
   - Information about mock behavior (for mocked tests)

## Coverage Configuration

Jest is configured to collect coverage information for:
- `controllers/**/*.ts`
- `routes/**/*.ts`
- `services.ts`

The coverage reports are available in HTML format in the `coverage/lcov-report` directory. 