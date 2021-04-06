package network.thunder.core.helper.blockchain;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.wallet.WalletTransaction;

import java.util.*;

public class MockBlockchainHelper implements BlockchainHelper {
    public Set<Sha256Hash> broadcastedTransaction = new HashSet<>();

    List<OnBlockCommand> blockListener = Collections.synchronizedList(new ArrayList<>());
    List<OnTxCommand> txListener = Collections.synchronizedList(new ArrayList<>());

    Wallet wallet;

    public MockBlockchainHelper () {
    }

    public MockBlockchainHelper (Wallet wallet) {
        this.wallet = wallet;
    }

    @Override
    public boolean broadcastTransaction (Transaction tx) {
        broadcastedTransaction.add(tx.getHash());

        //TODO not perfect yet - later we can connect multiple MockBlockchainHelper that will propagate tx across each other
        for (OnTxCommand onTxCommand : txListener) {
            if (onTxCommand.compare(tx)) {
                onTxCommand.execute(tx);
            }
        }

        if (wallet != null) {
            wallet.addWalletTransaction(new WalletTransaction(WalletTransaction.Pool.PENDING, tx));
        }

        return true;
    }

    @Override
    public void addTxListener (OnTxCommand executor) {
        txListener.add(executor);
    }

    @Override
    public void addBlockListener (OnBlockCommand executor) {
        blockListener.add(executor);
    }

    @Override
    public Transaction getTransaction (Sha256Hash hash) {
        return null;
    }
}
