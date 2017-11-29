package piuk.blockchain.android.ui.chooser;

import info.blockchain.wallet.contacts.data.Contact;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsDataManager;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.contacts.models.PaymentRequestType;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.util.StringUtils;
import timber.log.Timber;


public class AccountChooserPresenter extends BasePresenter<AccountChooserView> {

    private WalletAccountHelper walletAccountHelper;
    private StringUtils stringUtils;
    private ContactsDataManager contactsDataManager;

    private List<ItemAccount> itemAccounts = new ArrayList<>();

    @Inject
    AccountChooserPresenter(WalletAccountHelper walletAccountHelper,
                            StringUtils stringUtils,
                            ContactsDataManager contactsDataManager) {

        this.walletAccountHelper = walletAccountHelper;
        this.stringUtils = stringUtils;
        this.contactsDataManager = contactsDataManager;
    }

    @Override
    public void onViewReady() {
        PaymentRequestType paymentRequestType = getView().getPaymentRequestType();

        if (paymentRequestType == null) {
            throw new RuntimeException("Payment request type must be passed to the Account Chooser activity");
        }

        if (paymentRequestType.equals(PaymentRequestType.SEND)) {
            loadReceiveAccountsAndContacts();
        } else if (paymentRequestType.equals(PaymentRequestType.REQUEST)) {
            loadReceiveAccountsOnly();
        } else if (paymentRequestType.equals(PaymentRequestType.SHAPE_SHIFT)) {
            loadShapeShiftAccounts();
        } else {
            loadContactsOnly();
        }
    }

    private void loadReceiveAccountsAndContacts() {
        getCompositeDisposable().add(
                parseContactsList()
                        .flatMapObservable(contacts -> parseAccountList())
                        .flatMap(accounts -> parseImportedList())
                        .subscribe(
                                items -> getView().updateUi(itemAccounts),
                                Timber::e));
    }

    private void loadReceiveAccountsOnly() {
        getCompositeDisposable().add(
                parseAccountList()
                        .flatMap(accounts -> parseImportedList())
                        .subscribe(
                                list -> getView().updateUi(itemAccounts),
                                Timber::e));
    }

    private void loadShapeShiftAccounts() {
        getCompositeDisposable().add(
                parseAccountList()
                        .flatMap(accounts -> parseEthAccount())
                        .subscribe(
                                list -> getView().updateUi(itemAccounts),
                                Timber::e));
    }

    private void loadContactsOnly() {
        getCompositeDisposable().add(
                parseContactsList()
                        .subscribe(
                                items -> {
                                    if (!items.isEmpty()) {
                                        getView().updateUi(itemAccounts);
                                    } else {
                                        getView().showNoContacts();
                                    }
                                },
                                Timber::e));
    }

    @SuppressWarnings({"ConstantConditions", "Convert2streamapi"})
    private Single<List<Contact>> parseContactsList() {

        if (!getView().isContactsEnabled()) {
            return Single.just(new ArrayList<>());
        } else {

            return contactsDataManager.getContactList()
                    .filter(ContactsPredicates.filterByConfirmed())
                    .toList()
                    .doOnSuccess(contacts -> {
                        if (!contacts.isEmpty()) {
                            itemAccounts.add(new ItemAccount(stringUtils.getString(R.string.contacts_title), null, null, null, null));
                            for (Contact contact : contacts) {
                                itemAccounts.add(new ItemAccount(null, null, null, null, contact, null));
                            }
                        }
                    });
        }
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<List<ItemAccount>> parseAccountList() {
        return getAccountList()
                .doOnNext(accounts -> {
                    itemAccounts.add(new ItemAccount(stringUtils.getString(R.string.wallets), null, null, null, null));
                    itemAccounts.addAll(accounts);
                });
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<List<ItemAccount>> parseImportedList() {
        return getImportedList()
                .doOnNext(items -> {
                    if (!items.isEmpty()) {
                        itemAccounts.add(new ItemAccount(stringUtils.getString(R.string.imported_addresses), null, null, null, null));
                        itemAccounts.addAll(items);
                    }
                });
    }

    private Observable<List<ItemAccount>> parseEthAccount() {
        return getEthAccount()
                .doOnNext(ethModel -> {
                    itemAccounts.add(new ItemAccount(stringUtils.getString(R.string.ether), null, null, null, null));
                    itemAccounts.add(ethModel);
                })
                .map(ethModel -> itemAccounts);
    }

    private Observable<List<ItemAccount>> getAccountList() {
        ArrayList<ItemAccount> result = new ArrayList<>();
        result.addAll(walletAccountHelper.getHdAccounts());
        return Observable.just(result);
    }

    private Observable<List<ItemAccount>> getImportedList() {
        ArrayList<ItemAccount> result = new ArrayList<>();
        result.addAll(walletAccountHelper.getLegacyAddresses());
        return Observable.just(result);
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<ItemAccount> getEthAccount() {
        return Observable.just(walletAccountHelper.getEthAccount().get(0));
    }

}
