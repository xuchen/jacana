/**
 *
 */
package edu.jhu.jacana.align

import gnu.trove.map.hash.TIntObjectHashMap
import gnu.trove.map.hash.TObjectIntHashMap
import gnu.trove.map.hash.TIntIntHashMap
import edu.jhu.jacana.align.util.AlignerParams
import gnu.trove.TCollections

/**
 * A bidirectional map between a string and integer. Use for integerizing
 * features and classes. Index starts from 0.
 *
 * Performance note:
 * IntObjectHashMap: Mahout is 2-3x faster than Trove
 * ObjectIntHashMap: Trove is 2x faster than Mahout in gets
 *
 * In an alphabet mainly used for labels and feature names,
 * the most common operation is get(String), thus we use Trove.
 *
 * In feature vectors the most common operation is get(Int),
 * then we use Mahout/Colt
 *
 * @author Xuchen Yao
 *
 */
@SerialVersionUID(1L)
class Alphabet extends Serializable {
  private var int2str = TCollections.synchronizedMap(new TIntObjectHashMap[String]())
  private var str2int = TCollections.synchronizedMap(new TObjectIntHashMap[String]())

  def shallowCopy(a: Alphabet) {
    this.int2str = a.int2str
    this.str2int = a.str2int
  }

  def put(str: String): Int = {
    var size = 0
    int2str.synchronized {
      size = int2str.size
      int2str.put(size, str)
    }
    str2int.synchronized {
      str2int.put(str, size)
    }
    return size
  }

  def get(str: String): Int = {
    str2int.synchronized {
      if (!str2int.containsKey(str))
        return put(str)
      return str2int.get(str)
    }
  }

  def getWithoutPut(str: String): Int = str2int.synchronized{str2int.get(str)}

  def getString(i: Int): String = int2str.synchronized{int2str.get(i)}

  def getStrings(): Array[String] = int2str.synchronized{int2str.values(new Array[String](int2str.size())) }

  def size(): Int = { str2int.synchronized {str2int.size}}

  def contains(f: String): Boolean = str2int.synchronized{str2int.containsKey(f)}
}

//object FeatureAlphabet extends Alphabet {
//    
//}

/*
 * This object was originally used for the label(state) alphabet. But when coding oftentimes I forgot
 * to use this object to convert between word index (integer) and state id (integer).
 * So we discard using this object here for now, but hard code the mapping using the following convention:
 * NULL state: 0
 * state id = word index (starting from 1 to n, the length of target state)
 * later we might add "minus states": -1 to -n, those are basically NULL states but they encode the position
 * of last aligned-to index. Och & Nay 2003 had this idea originally.
 */

/*
 * A special alphabet designed only for using word indices (starting from 1) as states. 
 * Thus the mapping is between Int and Int.
 * The following rules are hard coded:
 * 1. NULL state always has a unique ID of 0. Assuming that there's always a NULL state, thus the default 
 * 		minimal size() of this object is always 1
 * 2. There's a special function totalStates(), which isn't necessarily equal to size(). This function
 * 		gives the longest word position we have seen plus 1. e.g., when the longest (target) sentence in training  
 * 		has 15 words, totalStates() returns 16: 15 states ranging {1,...,15} and one NULL (0) state.
 * 		For instance, for *all* training data, if the aligned indices only cover {2,3,7} (i,e, e.g., word 1 was
 * 		*never* aligned to) , then the state space is {0,2,3,7}.
 * 		size() returns 4 while totalStates() return 8.
 * 		
 * 		When doing forward-backward/Viterbi we have to set up numY = totalStates(), instead of size(). Namely,
 * 		the potential state space contains all *possible* states (including those never observed), while in
 * 		a standard CRF setting, the potential state space contains all *seen* states (think of POS tagging, you
 * 		only assign an existing POS tag to a word).
 */

@SerialVersionUID(1L)
class IndexLabelAlphabet extends Serializable {

  private val pos2id = TCollections.synchronizedMap(new TIntIntHashMap())
  private val id2pos = TCollections.synchronizedMap(new TIntIntHashMap())
  pos2id.put(0, 0)
  id2pos.put(0, 0)
  private var _freeze = false

  def freeze() { _freeze = true }

  private def put(pos: Int): Int = {
    if (_freeze) {
      return -1
    }
    pos2id.synchronized {
      if (pos2id.containsKey(pos))
        return pos2id.get(pos)
    }
    var size = -1
    id2pos.synchronized {
      size = id2pos.size
      id2pos.put(size, pos)
    }
    pos2id.synchronized {
      pos2id.put(pos, size)
    }
    return size
  }

  def get(pos: Int): Int = {
    if (_freeze) {
      pos2id.synchronized {
        if (pos2id.containsKey(pos))
          return pos2id.get(pos)
        else
          return -1
      }
    }
    pos2id.synchronized {
      if (!pos2id.containsKey(pos))
        return put(pos)
      return pos2id.get(pos)
    }
  }

  // note that id2pos is of different size than pos2id!
  def size(): Int = { id2pos.synchronized {id2pos.size}}

  def totalStates(): Int = {pos2id.synchronized {pos2id.keys().max+1}}

  def getNullState(): Int = this.get(0)

  /**
   * set the max *possible* state ID during training
   *
   * this can be called when loading the training data,
   * or not called at all, but the feature extractors
   * will automatically set the max possible state idx.
   */
  def setMaxStateIdx(tgtLen: Int) {
    getMergedStateIdx(tgtLen - 1, tgtLen, AlignerParams.maxTargetPhraseLen)
  }

  /**
   * get merged state index for multi-token phrases on the '''target''' side.
   *
   * when <code>span</code> == 1, <code>pos</code> is the state index;
   * otherwise, state index is mapped uniquely to indices larger than <code>tgtLen</code>.
   *
   * @param pos position start (starting from 1)
   * @param tgtLen length of the target sentence
   * @param span length of phrase
   */
  def getMergedStateIdx(pos: Int, tgtLen: Int, span: Int): Int = {
    var stateIdx = 0
    if (span == 1)
      stateIdx = pos
    else
      stateIdx = tgtLen + (pos - 1) * (AlignerParams.maxTargetPhraseLen - 1) + span - 1

    get(stateIdx)
    return stateIdx
  }

  def getMaxMergedStateIdx(tgtLen: Int): Int = {
    // max is the the second to the last one with a span of 2
    return getMergedStateIdx(tgtLen - 1, tgtLen, 2)
  }

  def getMergedStateID(pos: Int, tgtLen: Int, span: Int): Int = {
    return get(getMergedStateIdx(pos, tgtLen, span))
  }
  //def getStateFromWordIdx(i:Int): Int = this.get(i+1)
  //def getWordIdxStringFromState(i: Int): String = this.getString(i)
  //def getWordIdxStringFromState(i: Int): String = this.getString(i)
}

object IndexLabelAlphabet {
  final val NONE_STATE = -1
  final val NULL_STATE = 0

  /**
   * Given a state index <code>idx</code>, and target sentence length <code>tgtLen</code>,
   * map it back to the original target position and span (an Int tuple).
   *
   */
  def getPosAndSpanByStateIdx(idx: Int, tgtLen: Int): (Int, Int) = {
    var pos = if (idx > tgtLen) (idx - tgtLen) / (AlignerParams.maxTargetPhraseLen - 1) + 1 else idx
    var span = if (idx > tgtLen) (idx - tgtLen) % (AlignerParams.maxTargetPhraseLen - 1) + 1 else 1

    // idx > tgtLen is a must: when idx == tgtLen, span = 1 as well, we don't want to correct the offset
    if (span == 1 && idx > tgtLen) {
      // correct for modulus rounding offset
      span = AlignerParams.maxTargetPhraseLen
      pos -= 1
    }
    return (pos, span)
  }

  def main(args: Array[String]): Unit = {
    // assume sentence of length 10
    val alphabet = new IndexLabelAlphabet()
    AlignerParams.maxTargetPhraseLen = 3
    var tgtLen = 10
    println(alphabet.getMaxMergedStateIdx(tgtLen))
    println

    var idx = alphabet.getMergedStateIdx(2, tgtLen, 2)
    println(idx)
    println(getPosAndSpanByStateIdx(idx, tgtLen))
    println

    idx = alphabet.getMergedStateIdx(2, tgtLen, 3)
    println(idx)
    println(getPosAndSpanByStateIdx(idx, tgtLen))
    println

    idx = alphabet.getMergedStateIdx(8, tgtLen, 2)
    println(idx)
    println(getPosAndSpanByStateIdx(idx, tgtLen))
    println

    idx = alphabet.getMergedStateIdx(8, tgtLen, 3)
    println(idx)
    println(getPosAndSpanByStateIdx(idx, tgtLen))
    println

    idx = alphabet.getMergedStateIdx(9, tgtLen, 2)
    println(idx)
    println(getPosAndSpanByStateIdx(idx, tgtLen))
    println
    
    alphabet.put(4)
    println(alphabet.get(4))
    println(alphabet.totalStates)
    println(alphabet.size)
    println

  }
}