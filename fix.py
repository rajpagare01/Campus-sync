import sys

def fix_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    content = content.replace('jsonPath("$.', 'jsonPath("$.data.')
    content = content.replace('content().string("Event deleted successfully")', 'jsonPath("$.data").value("Event deleted successfully")')
    content = content.replace('content().string("Post deleted successfully")', 'jsonPath("$.data").value("Post deleted successfully")')
    content = content.replace('void getAllPosts', '@WithMockUser\n    void getAllPosts')
    content = content.replace('void getPostById', '@WithMockUser\n    void getPostById')
    content = content.replace('void getPostsByAuthor', '@WithMockUser\n    void getPostsByAuthor')
    content = content.replace('void getPostsWithMedia', '@WithMockUser\n    void getPostsWithMedia')
    content = content.replace('jsonPath("$[', 'jsonPath("$.data[')
    with open(filepath, 'w') as f:
        f.write(content)

fix_file(r'C:\Users\asus\Downloads\backend\backend\src\test\java\com\campussync\backend\Controller\EventControllerTest.java')
fix_file(r'C:\Users\asus\Downloads\backend\backend\src\test\java\com\campussync\backend\Controller\PostControllerTest.java')
fix_file(r'C:\Users\asus\Downloads\backend\backend\src\test\java\com\campussync\backend\Controller\UserProfileControllerTest.java')
