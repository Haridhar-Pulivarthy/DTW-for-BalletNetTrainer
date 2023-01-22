# DTW-for-BalletNetTrainer

A modified recursive Dynamic Time Warping algorithm I made for the BalletNetTrainer application.

The problem I was trying to solve:
  A professional ballet dancer may execute a pirouette over the course of 20 frames.
  An amateur ballet dancer may execute a pirouette over the course of 30 frames.
  However, this does not mean that the amateur dancer made a mistake or used improper technique.
  They simply executed the move slower, and they should not necessarily be penalized for it.
  However, our program compares these videos frame by frame.
  For example, it may compare frame 10 of the professional video with frame 10 of the amateur video.
  Even though both dancers are executing the move correctly, the professional is halfway done whereas
  the amateur is only a third of the way through.
  Therefore, their poses in frame 10 are vastly different, and the program will penalize the amateur
  heavily.
 The solution I used:
  I implemented a dynamic time warping algorithm, which is an algorithm that measures the similarity
  between two time series, which may vary in speed.
  The indexes in my algorithm are the frames of the videos, and we calculated the similarity between
  the frames by taking the 3-D joint coordinates (identified by OpenPose), and calculating the angles
  between each limb. This is necessary because dancers have varying limb lengths. Anyway, we compare
  the corresponding angles of each joint in each frame to determine how similar one frame is to
  another. Due to the hundreds of comparisons per second of video data, I had to implement a recursive
  version of the DTW algorithm, which is slightly less accurate but quite a bit faster.
  This recursive implementation was first created by Stan Salvador and Philip Chan in their paper,
  FastDTW: Toward Accurate Dynamic Time Warping in Linear Time and Space.
  https://cs.fit.edu/~pkc/papers/tdm04.pdf
  All credit goes to them for the algorithm.
  
  Pre- and post-processing (e.g. the angle parser) were created by me.
