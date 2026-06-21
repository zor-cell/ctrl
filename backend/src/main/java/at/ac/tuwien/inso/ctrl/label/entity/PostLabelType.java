package at.ac.tuwien.inso.ctrl.label.entity;

import lombok.Getter;

@Getter
public enum PostLabelType {
  POPULAR("🔥", "Popular", "More than 5 likes"),
  RECENT("🕒", "Recent", "Created less than 6 hours ago"),
  OLD_POST("🗂️", "Old Post", "Created more than 6 months ago"),
  AI_GENERATED("🤖", "AI Generated", "Creator marked this as AI-generated"),
  HIGHLY_DISCUSSED("💬", "Highly Discussed", "More than 5 comments"),
  TRUSTED_SOURCE("✅", "Trusted Source", "The community considers this source reliable"),
  DISPUTED_SOURCE(
      "⚠️", "Disputed Source", "The community has raised reliability concerns about this source"),
  EXTENDED_NETWORK("❤️", "Extended Network", "Posted by someone your follows/friends follow"),
  MASTODON("Ⓜ️", "Mastodon", "This post is from Mastodon"),
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
