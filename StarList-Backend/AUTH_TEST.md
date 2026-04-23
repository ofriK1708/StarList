# Testing Cognito JWT Authentication

## 1. Start the backend
Run with the `dev` profile (default). You should see the app start without errors.

---

## 2. Verify public endpoints work (no token needed)

```bash
# Health check — should return 200
curl http://localhost:8080/check/health

# Create user — should return 201 (or 409 if already exists)
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","cognitoUserId":"00000000-0000-0000-0000-000000000001","displayName":"Test"}'
```

---

## 3. Get a real JWT token from the browser

1. Start the frontend (`npm run dev`)
2. Log in with a real Cognito account
3. Open **DevTools → Network tab**
4. Make any action that triggers an API call (e.g. navigate to habits)
5. Click on any request to your backend
6. Under **Request Headers**, copy the value of `Authorization` — it looks like:
   ```
   Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

Alternatively, paste this in the **browser console** while logged in:

```javascript
const { fetchAuthSession } = await import('aws-amplify/auth');
const session = await fetchAuthSession();
console.log(session.tokens.idToken.toString());
```

---

## 4. Test a protected endpoint WITH the token

```bash
TOKEN="eyJhbGciOiJSUzI1NiIs..."   # paste your token here

# Should return 200 with user data
curl http://localhost:8080/users/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

## 5. Verify protected endpoints reject requests without a token

```bash
# Should return 401 Unauthorized
curl -v http://localhost:8080/users/1
```

---

## 6. Verify an invalid/expired token is rejected

```bash
# Should return 401
curl http://localhost:8080/users/1 \
  -H "Authorization: Bearer thisisnotavalidtoken"
```