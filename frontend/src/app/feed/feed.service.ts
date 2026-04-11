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
        createdAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
        caption: 'Golden hour in the nature. Sometimes you just have to put the phone down ✨',
        isPublic: false,
        whyExplanation: 'This post is very recent and you are interested in the topic Art & Culture.',
        upvotes: 0,
        downvotes: 0,
        likes: 142,
        labelIds: ['ai-generated', 'recent'],
        topicIds: ['Art & Culture'],
        sourceIds: ['assets/posts/post3.jpeg'],
        comments: [
          {
            commentId: 'c1-1',
            userId: 'user-4',
            username: 'sara.wanderlust',
            userProfilePic: 'assets/users/user4ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 55 * 60 * 1000).toISOString(),
            content: 'This is absolutely stunning 😍 Where was this taken?',
            parentCommentId: null,
          },
          {
            commentId: 'c1-2',
            userId: 'user-1',
            username: 'leo.photo',
            userProfilePic: 'assets/users/user1ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 50 * 60 * 1000).toISOString(),
            content: 'Thanks! It was somewhere in the Austrian Alps, totally spontaneous stop 🏔️',
            parentCommentId: 'c1-1',
          },
          {
            commentId: 'c1-3',
            userId: 'user-5',
            username: 'max.clicks',
            userProfilePic: 'assets/users/user5ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 48 * 60 * 1000).toISOString(),
            content: 'The colors are unreal. What camera do you shoot on?',
            parentCommentId: null,
          },
          {
            commentId: 'c1-4',
            userId: 'user-1',
            username: 'leo.photo',
            userProfilePic: 'assets/users/user1ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 44 * 60 * 1000).toISOString(),
            content: 'Sony A7IV, but honestly the light did all the work here 😄',
            parentCommentId: 'c1-3',
          },
          {
            commentId: 'c1-5',
            userId: 'user-6',
            username: 'nina.art',
            userProfilePic: 'assets/users/user6ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
            content: 'Saving this as my new wallpaper, hope that\'s ok 🙏',
            parentCommentId: null,
          },
          {
            commentId: 'c1-6',
            userId: 'user-7',
            username: 'tom.frames',
            userProfilePic: 'assets/users/user7ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
            content: 'Genuinely one of the best shots I\'ve seen this week.',
            parentCommentId: null,
          },
        ],
      },
      {
        postId: 'post-2',
        userId: 'user-2',
        username: 'maria.codes',
        userProfilePic: 'assets/users/user2ProfilePic.jpeg',
        createdAt: new Date(Date.now() - 3 * 60 * 1000).toISOString(),
        caption: 'Just shipped a new feature in our open-source project 🚀 Contribution is key.',
        isPublic: false,
        whyExplanation: 'You frequently engage with tech and software engineering content.',
        upvotes: 0,
        downvotes: 0,
        likes: 87,
        labelIds: ['recent'],
        topicIds: ['Technology'],
        sourceIds: ['assets/posts/post1.png'],
        comments: [
          {
            commentId: 'c2-1',
            userId: 'user-5',
            username: 'max.clicks',
            userProfilePic: 'assets/users/user5ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 2 * 60 * 1000).toISOString(),
            content: 'Congrats on the ship! What\'s the feature about?',
            parentCommentId: null,
          },
          {
            commentId: 'c2-2',
            userId: 'user-2',
            username: 'maria.codes',
            userProfilePic: 'assets/users/user2ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 90 * 1000).toISOString(),
            content: 'Real-time collaboration support — still in beta but working great so far!',
            parentCommentId: 'c2-1',
          },
          {
            commentId: 'c2-3',
            userId: 'user-8',
            username: 'dev.jonas',
            userProfilePic: 'assets/users/user8ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 60 * 1000).toISOString(),
            content: 'Is there a GitHub link? Would love to contribute.',
            parentCommentId: null,
          },
          {
            commentId: 'c2-4',
            userId: 'user-2',
            username: 'maria.codes',
            userProfilePic: 'assets/users/user2ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 45 * 1000).toISOString(),
            content: 'Yes! Check the link in my bio — PRs are very welcome 🙌',
            parentCommentId: 'c2-3',
          },
          {
            commentId: 'c2-5',
            userId: 'user-9',
            username: 'lena.dev',
            userProfilePic: 'assets/users/user9ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 30 * 1000).toISOString(),
            content: 'Open source contributions are so underrated. Keep it up!',
            parentCommentId: null,
          },
          {
            commentId: 'c2-6',
            userId: 'user-4',
            username: 'sara.wanderlust',
            userProfilePic: 'assets/users/user4ProfilePic.jpeg',
            createdAt: new Date(Date.now() - 10 * 1000).toISOString(),
            content: 'Following for more updates on this 👀',
            parentCommentId: null,
          },
        ],
      },
      {
        postId: 'post-3',
        userId: 'user-3',
        username: 'foodie.hans',
        userProfilePic: 'assets/users/user3ProfilePic.jpeg',
        createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
        caption: 'Homemade ramen on a rainy Sunday. Pure comfort in a bowl 🍜',
        isPublic: false,
        whyExplanation: 'Food & lifestyle content matches your saved topics and past interactions.',
        upvotes: 0,
        downvotes: 0,
        likes: 213,
        labelIds: [],
        topicIds: ['Food & Lifestyle'],
        sourceIds: ['assets/posts/post2.webp'],
        comments: [

        ],
      },
    ];
  }

  async getTagesschauPosts(): Promise<Post[]> {
    try {
      // Schritt 1: Korrekte ID holen
      const lookupRes = await fetch('https://ard.social/api/v1/accounts/lookup?acct=tagesschau');
      const account = await lookupRes.json();

      if (!account?.id) return [];

      // Schritt 2: Posts laden
      const statusRes = await fetch(
        `https://ard.social/api/v1/accounts/${account.id}/statuses?limit=20&exclude_replies=true`
      );
      const data = await statusRes.json();

      if (!Array.isArray(data)) return [];

      return data
        .filter((s: any) => s.content && !s.reblog)
        .map((status: any) => ({
          postId: status.id,
          userId: status.account.id,
          username: status.account.display_name,
          userProfilePic: status.account.avatar,
          createdAt: status.created_at,
          caption: status.content.replace(/<[^>]+>/g, '').trim(),
          isPublic: true,
          upvotes: 0,
          downvotes: 0,
          likes: status.favourites_count,
          sourceIds: status.media_attachments?.length > 0
            ? status.media_attachments
              .filter((m: any) => m.type === 'image')
              .map((m: any) => m.url)
            : status.card?.image
              ? [status.card.image]
              : [],
          labelIds: ['mastodon.social'],
          topicIds: status.tags.map((t: any) => t.name),
          comments: [],
          whyExplanation: 'Latest news from Tagesschau',
        }));
    } catch (e) {
      console.error('Mastodon fetch failed', e);
      return [];
    }
  }

  async getBBCNewsPosts(): Promise<Post[]> {
    try {
      const lookupRes = await fetch('https://mastodon.social/api/v1/accounts/lookup?acct=BBCNews@flipboard.com');
      const account = await lookupRes.json();

      if (!account?.id) return [];

      const statusRes = await fetch(
        `https://mastodon.social/api/v1/accounts/${account.id}/statuses?limit=20&exclude_replies=true`
      );
      const data = await statusRes.json();

      if (!Array.isArray(data)) return [];

      return data
        .filter((s: any) => s.content && !s.reblog)
        .map((status: any) => ({
          postId: status.id,
          userId: status.account.id,
          username: status.account.display_name,
          userProfilePic: status.account.avatar,
          createdAt: status.created_at,
          caption: status.content.replace(/<[^>]+>/g, '').trim(),
          isPublic: true,
          upvotes: 0,
          downvotes: 0,
          likes: 0,
          sourceIds: status.media_attachments?.length > 0
            ? status.media_attachments
              .filter((m: any) => m.type === 'image')
              .map((m: any) => m.url)
            : status.card?.image
              ? [status.card.image]
              : [],
          labelIds: ['mastodon.social'],
          topicIds: status.tags.map((t: any) => t.name),
          comments: [],
          whyExplanation: 'Latest news from BBC News',
        }));
    } catch (e) {
      console.error('BBC fetch failed', e);
      return [];
    }
  }
}
