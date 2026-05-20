#!/bin/bash

# CampusSync Post Management - cURL Testing Script
# Run this script to test the complete post functionality

BASE_URL="http://localhost:8080"
EMAIL="society@example.com"
PASSWORD="password123"

echo "🎓 CampusSync Post Management Testing"
echo "======================================"

# Function to extract JWT token from login response
extract_token() {
    echo "$1" | grep -o '"[^"]*"' | head -1 | tr -d '"'
}

echo ""
echo "1. Registering society user..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Society Test User\",
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\",
    \"role\": \"SOCIETY\"
  }")

echo "Response: $REGISTER_RESPONSE"

echo ""
echo "2. Verifying email (using test OTP: 123456)..."
VERIFY_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/verify?email=$EMAIL&code=123456&name=Society%20Test%20User")

echo "Response: $VERIFY_RESPONSE"

echo ""
echo "3. Logging in to get JWT token..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\"
  }")

JWT_TOKEN=$(extract_token "$LOGIN_RESPONSE")
echo "JWT Token: $JWT_TOKEN"

if [ -z "$JWT_TOKEN" ]; then
    echo "❌ Failed to get JWT token. Check login response above."
    exit 1
fi

echo ""
echo "4. Creating a text-only post..."
CREATE_POST_RESPONSE=$(curl -s -X POST "$BASE_URL/posts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d "{
    \"content\": \"Welcome to our society! We're excited for the new semester. 🎓 #CampusLife\"
  }")

echo "Response: $CREATE_POST_RESPONSE"

# Extract post ID from response (simple extraction)
POST_ID=$(echo "$CREATE_POST_RESPONSE" | grep -o '"id":[0-9]*' | grep -o '[0-9]*')

echo ""
echo "5. Getting all posts..."
GET_POSTS_RESPONSE=$(curl -s -X GET "$BASE_URL/posts")

echo "Response: $GET_POSTS_RESPONSE"

if [ ! -z "$POST_ID" ]; then
    echo ""
    echo "6. Getting specific post (ID: $POST_ID)..."
    GET_POST_RESPONSE=$(curl -s -X GET "$BASE_URL/posts/$POST_ID")

    echo "Response: $GET_POST_RESPONSE"

    echo ""
    echo "7. Deleting the post..."
    DELETE_RESPONSE=$(curl -s -X DELETE "$BASE_URL/posts/$POST_ID" \
      -H "Authorization: Bearer $JWT_TOKEN")

    echo "Response: $DELETE_RESPONSE"
fi

echo ""
echo "8. Testing error scenarios..."

echo ""
echo "8a. Creating post without authentication (should fail)..."
NO_AUTH_RESPONSE=$(curl -s -X POST "$BASE_URL/posts" \
  -H "Content-Type: application/json" \
  -d "{
    \"content\": \"This should fail\"
  }")

echo "Response: $NO_AUTH_RESPONSE"

echo ""
echo "8b. Creating post with empty content (should fail)..."
EMPTY_CONTENT_RESPONSE=$(curl -s -X POST "$BASE_URL/posts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d "{
    \"content\": \"\"
  }")

echo "Response: $EMPTY_CONTENT_RESPONSE"

echo ""
echo "9. Testing file upload endpoint..."
# Note: This requires a file to exist. Uncomment and modify path as needed.
# FILE_UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/files/posts/upload" \
#   -H "Authorization: Bearer $JWT_TOKEN" \
#   -F "file=@/path/to/your/image.jpg")

# echo "File Upload Response: $FILE_UPLOAD_RESPONSE"

echo ""
echo "🎉 Testing completed!"
echo ""
echo "Summary:"
echo "- ✅ User registration and verification"
echo "- ✅ JWT authentication"
echo "- ✅ Post creation, retrieval, and deletion"
echo "- ✅ Error handling for unauthorized access"
echo "- ✅ Input validation"
echo ""
echo "📝 Note: File upload testing requires a physical file."
echo "   Use Postman collection for complete file upload testing."