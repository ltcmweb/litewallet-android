package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.TxItem;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.TxManager;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;
import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class FragmentTransactionItem extends Fragment {
    public TextView mTitle;
    private TextView mDescriptionText;
    private TextView mSubHeader;
    private TextView mConfirmationText;
    private TextView mAvailableSpend;
    private EditText mCommentText;
    private TextView mAmountText;
    private TextView mAddressText;
    private TextView mDateText;
    private TextView mToFromBottom;
    private TextView mTxHash;
    private TxItem item;
    private LinearLayout signalLayout;
    private ImageButton close;
    private String oldComment;
    private TextView mTxHashLink;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.transaction_details_item, container, false);
        signalLayout = (LinearLayout) rootView.findViewById(R.id.signal_layout);
        mTitle = (TextView) rootView.findViewById(R.id.title);
        mDescriptionText = (TextView) rootView.findViewById(R.id.description_text);
        mSubHeader = (TextView) rootView.findViewById(R.id.sub_header);
        mCommentText = (EditText) rootView.findViewById(R.id.comment_text);
        mAmountText = (TextView) rootView.findViewById(R.id.amount_text);
        mAddressText = (TextView) rootView.findViewById(R.id.address_text);
        mDateText = (TextView) rootView.findViewById(R.id.date_text);
        mToFromBottom = (TextView) rootView.findViewById(R.id.to_from);
        mConfirmationText = (TextView) rootView.findViewById(R.id.confirmation_text);
        mAvailableSpend = (TextView) rootView.findViewById(R.id.available_spend);
        mTxHash = (TextView) rootView.findViewById(R.id.tx_hash);
        mTxHashLink = (TextView) rootView.findViewById(R.id.tx_hash_link);
        close = (ImageButton) rootView.findViewById(R.id.close_button);

        //TODO: all views are using the layout of this button. Views should be refactored without it
        // Hiding until layouts are built.
        ImageButton faq = (ImageButton) rootView.findViewById(R.id.faq_button);

        signalLayout.setOnTouchListener(new SlideDetector(signalLayout, this::close));

        rootView.setOnClickListener(v -> {
            if (!BRAnimator.isClickAllowed()) return;
            close();
        });
        close.setOnClickListener(v -> close());

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fillTexts();
    }

    private void fillTexts() {
        //get the current iso
        String iso = BRSharedPrefs.getPreferredLTC(getActivity()) ? "LTC" : BRSharedPrefs.getIso(getContext());

        //get the tx amount
        BigDecimal txAmount = new BigDecimal(item.getReceived() - item.getSent()).abs();
        //see if it was sent
        boolean sent = item.getReceived() - item.getSent() < 0;

        //calculated and formatted amount for iso
        String amountWithFee = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, txAmount));
        String amount = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, item.getFee() == -1 ? txAmount : txAmount.subtract(new BigDecimal(item.getFee()))));
        //calculated and formatted fee for iso
        String fee = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(item.getFee())));
        //description (Sent $24.32 ....)
        Spannable descriptionString = sent ? new SpannableString(String.format(getString(R.string.TransactionDetails_sent), amountWithFee)) : new SpannableString(String.format(getString(R.string.TransactionDetails_received), amount));

        String startingBalance = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(sent ? item.getBalanceAfterTx() + txAmount.longValue() : item.getBalanceAfterTx() - txAmount.longValue())));
        String endingBalance = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(item.getBalanceAfterTx())));
        String commentString = item.comment;
        String sb = String.format(getString(R.string.Transaction_starting), startingBalance);
        String eb = String.format(getString(R.string.Transaction_ending), endingBalance);
        String amountString = String.format("%s %s\n\n%s\n%s", amount, item.getFee() == -1 ? "" : String.format(getString(R.string.Transaction_fee), fee), sb, eb);
        if (sent) amountString = "-" + amountString;
        String addr = item.getTo().length > 0 ? item.getTo()[0] : "(unknown)";
        String toFrom = sent ? String.format(getString(R.string.TransactionDetails_to), addr) : String.format(getString(R.string.TransactionDetails_from), addr);

        mTxHash.setText(item.getTxHashHexReversed());
        mTxHashLink.setOnClickListener(view -> {
            close();
            String txUrl = BRConstants.BLOCK_EXPLORER_BASE_URL + item.getTxHashHexReversed();
            Timber.d("timber: txUrl = %s", txUrl);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(txUrl));
            startActivity(browserIntent);
            getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
        });

        int level = getLevel(item);

        boolean availableForSpend = false;
        String percentage = "";
        switch (level) {
            case 0:
                percentage = "0%";
                break;
            case 1:
                percentage = "20%";
                break;
            case 2:
                percentage = "40%";
                break;
            case 3:
                percentage = "60%";
                availableForSpend = true;
                break;
            case 4:
                percentage = "80%";
                availableForSpend = true;
                break;
            case 5:
                percentage = "100%";
                availableForSpend = true;
                break;
        }

        if (getConfirms(item) < item.lockedFor) {
            availableForSpend = false;
        }

        boolean removeView = sent || !availableForSpend;
        Timber.d("timber: fillTexts: removeView : %s", removeView);
        if (!removeView) {
            mAvailableSpend.setText(getString(R.string.Transaction_available));
        } else {
            mAvailableSpend.setText("");
            signalLayout.removeView(mAvailableSpend);
        }

        if (level == 6) {
            mConfirmationText.setText(getString(R.string.Transaction_complete));
        } else {
            mConfirmationText.setText(String.format("%s", percentage));
        }

        if (!item.isValid())
            mConfirmationText.setText(getString(R.string.Transaction_invalid));

        mToFromBottom.setText(sent ? getString(R.string.TransactionDirection_to) : getString(R.string.TransactionDirection_address));
        mDateText.setText(getFormattedDate(item.getTimeStamp()));
        mDescriptionText.setText(TextUtils.concat(descriptionString));
        mSubHeader.setText(toFrom);
        mCommentText.setText(commentString);

        mAmountText.setText(amountString);
        mAddressText.setText(addr);
    }

    private int getConfirms(TxItem item) {
        int blockHeight = item.getBlockHeight();
        return blockHeight == 0 ? 0 : BRSharedPrefs.getLastBlockHeight(getContext()) - blockHeight + 1;
    }

    private int getLevel(TxItem item) {
        int confirms = getConfirms(item);
        int level;
        if (confirms <= 0) {
            int relayCount = BRPeerManager.getRelayCount(item.getTxHash());
            if (relayCount <= 0)
                level = 0;
            else if (relayCount == 1)
                level = 1;
            else
                level = 2;
        } else {
            if (confirms == 1)
                level = 3;
            else if (confirms == 2)
                level = 4;
            else if (confirms == 3)
                level = 5;
            else
                level = 6;
        }
        return level;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        oldComment = mCommentText.getText().toString();
    }

    @Override
    public void onPause() {
        String comment = mCommentText.getText().toString();
        final Activity app = getActivity();
        if (!comment.equals(oldComment)) {
            final TxMetaData md = new TxMetaData();
            md.comment = comment;
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    KVStoreManager.getInstance().putTxMetaData(app, md, item.getTxHash());
                    TxManager.getInstance().updateTxList(app);
                }
            });
        }
        oldComment = null;
        Utils.hideKeyboard(app);
        super.onPause();
    }

    public static FragmentTransactionItem newInstance(TxItem item) {
        FragmentTransactionItem f = new FragmentTransactionItem();
        f.setItem(item);
        return f;
    }

    public void setItem(TxItem item) {
        this.item = item;
    }

    private String getFormattedDate(long timeStamp) {

        Date currentLocalTime = new Date(timeStamp == 0 ? System.currentTimeMillis() : timeStamp * 1000);

        SimpleDateFormat date1 = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat date2 = new SimpleDateFormat("HH:mm a", Locale.getDefault());

        String str1 = date1.format(currentLocalTime);
        String str2 = date2.format(currentLocalTime);

        return str1 + " " + String.format(getString(R.string.TransactionDetails_from), str2);
    }

    private String getShortAddress(String addr) {
        String p1 = addr.substring(0, 5);
        String p2 = addr.substring(addr.length() - 5, addr.length());
        return p1 + "..." + p2;
    }

    private void close() {
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof FragmentTransactionDetails) {
            ((FragmentTransactionDetails) parentFragment).close();
        }
    }
}
