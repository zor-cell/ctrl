import { Injectable } from '@angular/core';
import { Post } from './dto/Post';

@Injectable({ providedIn: 'root' })
export class FeedService {

  getMockPosts(): Post[] {
    return [
      {
        postId: 'post-1',
        userId: 'user-1',
        username: 'leo.photo',
        userProfilePic: 'assets/users/user1ProfilePic.jpeg',
        createdAt: new Date(Date.now() - 1 * 60 * 60 * 1000).toISOString(), // 1h ago
        caption: 'Golden hour in the nature. Sometimes you just have to put the phone down ✨',
        isPublic: false,
        whyExplanation: 'This post is very recent and you are interested in the topic Art & Culture.',
        upvotes: 0,
        downvotes: 0,
        likes: 142,
        labelIds: ['photography', 'nature'],
        topicIds: ['Art & Culture'],
        sourceIds: ['assets/posts/post1.png'],
      },
      {
        postId: 'post-2',
        userId: 'user-2',
        username: 'maria.codes',
        userProfilePic: 'assets/users/user2ProfilePic.jpeg',
        createdAt: new Date(Date.now() - 3 * 60 * 1000).toISOString(), // 3 mins ago
        caption: 'Just shipped a new feature in our open-source project 🚀 Contribution is key.',
        isPublic: false,
        whyExplanation: 'You frequently engage with tech and software engineering content.',
        upvotes: 0,
        downvotes: 0,
        likes: 87,
        labelIds: ['tech', 'opensource'],
        topicIds: ['Technology'],
        sourceIds: ['assets/posts/post2.png'],
      },
      {
        postId: 'post-3',
        userId: 'user-3',
        username: 'foodie.hans',
        userProfilePic: 'assets/users/user3ProfilePic.jpeg',
        createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(), // 2d ago
        caption: 'Homemade ramen on a rainy Sunday. Pure comfort in a bowl 🍜',
        isPublic: false,
        whyExplanation: 'Food & lifestyle content matches your saved topics and past interactions.',
        upvotes: 0,
        downvotes: 0,
        likes: 213,
        labelIds: ['food', 'cooking'],
        topicIds: ['Food & Lifestyle'],
        sourceIds: ['assets/posts/post3.jpeg'],
      },
    ];
  }
}
