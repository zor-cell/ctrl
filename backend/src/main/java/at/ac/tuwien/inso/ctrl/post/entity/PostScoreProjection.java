package at.ac.tuwien.inso.ctrl.post.entity;

//this is a projection interface to get both post data and recommendation scoring in one class
public interface PostScoreProjection {
    String getPostId();

    Double getScoreInteraction();
    Double getScoreTopic();
    Double getScoreRecency();
    Double getScoreTrending();
    Double getScoreSource();
    Double getScoreExploration();
    Double getTotalScore();
}
