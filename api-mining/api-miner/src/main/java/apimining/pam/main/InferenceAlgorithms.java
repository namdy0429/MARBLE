package apimining.pam.main;

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import apimining.pam.sequence.Sequence;
import apimining.pam.transaction.Transaction;

/** Container class for Inference Algorithms */
public class InferenceAlgorithms {

	/** Interface for the different inference algorithms */
	public interface InferenceAlgorithm {
		public HashSet<Sequence> infer(final Transaction transaction);
	}

	/**
	 * Infer ML parameters to explain transaction using greedy algorithm and
	 * store in covering. Sequences *may not* overlap.
	 * <p>
	 * !! Assumes *no overlap* !! i.e. subseqs in covering are pairwise disjoint
	 */
	public static class InferGreedy implements InferenceAlgorithm, Serializable {
		private static final long serialVersionUID = 9173178089235828142L;

		@Override
		public HashSet<Sequence> infer(final Transaction transaction) {

			final HashSet<Sequence> covering = new HashSet<>();
			int lenCovering = 0;
			final int transactionSize = transaction.size();
			final BitSet coveredItems = new BitSet(transactionSize);

			final HashMap<Sequence, Double> cachedSequences = transaction.getCachedSequences();
			while (coveredItems.cardinality() != transactionSize) {

				double minCostPerItem = Double.POSITIVE_INFINITY;
				Sequence bestSeq = null;
				BitSet bestSeqCoveredItems = null;

				for (final Entry<Sequence, Double> entry : cachedSequences.entrySet()) {

					// Get sequence
					final Sequence seq = entry.getKey();

					// Ignore sequences which already cover
					if (covering.contains(seq))
						continue;

					// How many additional items does sequence cover?
					final BitSet seqCoveredItems = transaction.getCovered(seq, coveredItems);
					// Ignore sequences which don't cover anything
					if (seqCoveredItems.isEmpty())
						continue;

					final double cost = -Math.log(entry.getValue())
							+ sumLogRange(lenCovering + 1, lenCovering + seq.size()) - sumLogRange(1, seq.size());
					final double costPerItem = cost / seq.size();

					if (costPerItem < minCostPerItem) {
						minCostPerItem = costPerItem;
						bestSeq = seq;
						bestSeqCoveredItems = seqCoveredItems;
					}

				}

				if (bestSeq != null) {
					covering.add(bestSeq);
					lenCovering += bestSeq.size();
					coveredItems.or(bestSeqCoveredItems);
				} else { // Fill in incomplete coverings with singletons
					int index = 0;
					while (coveredItems.cardinality() != transactionSize) {
						index = coveredItems.nextClearBit(index);
						final Sequence seq = new Sequence(transaction.get(index));
						recursiveSetOccurrence(seq, covering);
						covering.add(seq);
						coveredItems.set(index);
					}
					return covering;
				}

			}
			return covering;
		}

		private double sumLogRange(final int a, final int b) {
			double sum = 0;
			for (int i = a; i <= b; i++)
				sum += Math.log(i);
			return sum;
		}

		private void recursiveSetOccurrence(final Sequence seq, final HashSet<Sequence> seenItems) {
			if (seenItems.contains(seq)) {
				seq.incrementOccurence();
				recursiveSetOccurrence(seq, seenItems);
			}
		}

	}

	private InferenceAlgorithms() {

	}

}
