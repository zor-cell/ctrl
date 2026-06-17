package at.ac.tuwien.inso.ctrl.label.entity;

import lombok.Getter;

@Getter
public enum PostLabelType {
  POPULAR("🔥", "Popular", "Top 5% upvotes of all posts"),
  RECENT("🕒", "Recent", "Created less than 6 hours ago"),
  OLD_POST("🗂️", "Old Post", "Created more than 6 months ago"),
  AI_GENERATED("🤖", "AI Generated", "Creator marked this as AI-generated"),
  HIGHLY_DISCUSSED("💬", "Highly Discussed", "More than 100 comments"),
  TRUSTED_SOURCE("✅", "Trusted Source", "High Wilson score with enough votes"),
  DISPUTED_SOURCE("⚠️", "Disputed Source", "Low Wilson score with enough votes"),
  EXTENDED_NETWORK("❤️", "Extended Network", "Posted by someone your follows follow"),
  MASTODON("Ⓜ️", "Mastodon", "Post from Mastodon"),
  POST_REPORTED("🚩", "Reported", "You have reported this post");

  private final String emoji;
  private final String name;
  private final String explanation;

  PostLabelType(String emoji, String name, String explanation) {
    this.emoji = emoji;
    this.name = name;
    this.explanation = explanation;
  }
}
