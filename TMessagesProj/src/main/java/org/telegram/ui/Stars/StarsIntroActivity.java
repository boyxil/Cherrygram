package org.telegram.ui.Stars;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatPluralString;
import static org.telegram.messenger.LocaleController.formatPluralStringComma;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.Stars.StarsIntroActivity.StarsTransactionView.getPlatformDrawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.ReplacementSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BillingController;
import org.telegram.messenger.BirthdayController;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.WebFile;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.AvatarSpan;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.SessionCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BottomSheetWithRecyclerListView;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.FireworksOverlay;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.LoadingSpan;
import org.telegram.ui.Components.OutlineTextContainerView;
import org.telegram.ui.Components.Premium.GLIcon.GLIconRenderer;
import org.telegram.ui.Components.Premium.GLIcon.GLIconTextureView;
import org.telegram.ui.Components.Premium.GLIcon.Icon3D;
import org.telegram.ui.Components.Premium.StarParticlesView;
import org.telegram.ui.Components.Premium.boosts.UserSelectorBottomSheet;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.StarAppsSheet;
import org.telegram.ui.Components.TableView;
import org.telegram.ui.Components.Text;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.Components.ViewPagerFixed;
import org.telegram.ui.Components.spoilers.SpoilerEffect2;
import org.telegram.ui.GradientHeaderActivity;
import org.telegram.ui.ImageReceiverSpan;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;
import org.telegram.ui.Stories.recorder.HintView2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class StarsIntroActivity extends GradientHeaderActivity implements NotificationCenter.NotificationCenterDelegate {

    private StarsBalanceView balanceView;

    private FrameLayout aboveTitleView;
    private GLIconTextureView iconTextureView;

    private StarsTransactionsLayout transactionsLayout;
    private View emptyLayout;
    private FireworksOverlay fireworksOverlay;

    public StarsIntroActivity() {
        setWhiteBackground(true);
    }

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starOptionsLoaded);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starBalanceUpdated);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starTransactionsLoaded);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starSubscriptionsLoaded);
        StarsController.getInstance(currentAccount).invalidateTransactions(true);
        StarsController.getInstance(currentAccount).invalidateSubscriptions(true);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starOptionsLoaded);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starBalanceUpdated);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starTransactionsLoaded);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starSubscriptionsLoaded);
    }

    private boolean hadTransactions;
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (
            id == NotificationCenter.starOptionsLoaded
//            || id == NotificationCenter.starTransactionsLoaded
        ) {
            saveScrollPosition();
            if (adapter != null) {
                adapter.update(true);
            }
            if (savedScrollPosition == 0 && savedScrollOffset < 0) {
                savedScrollOffset = 0;
            }
            applyScrolledPosition();
        } else if (id == NotificationCenter.starTransactionsLoaded) {
            final StarsController c = StarsController.getInstance(currentAccount);
            if (hadTransactions != c.hasTransactions()) {
                hadTransactions = c.hasTransactions();
                saveScrollPosition();
                if (adapter != null) {
                    adapter.update(true);
                }
                if (savedScrollPosition == 0 && savedScrollOffset < 0) {
                    savedScrollOffset = 0;
                }
                applyScrolledPosition();
            }
        } else if (id == NotificationCenter.starSubscriptionsLoaded) {
            if (adapter != null) {
                adapter.update(true);
            }
        }
    }

    @Override
    public int getNavigationBarColor() {
        return Theme.getColor(Theme.key_dialogBackgroundGray);
    }

    @Override
    public View createView(Context context) {
        useFillLastLayoutManager = false;
        particlesViewHeight = dp(32 + 190 + 16);
        transactionsLayout = new StarsTransactionsLayout(context, currentAccount, 0, getClassGuid(), getResourceProvider());
        emptyLayout = new View(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int firstViewHeight;
                if (StarsIntroActivity.this.isLandscapeMode) {
                    firstViewHeight = StarsIntroActivity.this.statusBarHeight + actionBar.getMeasuredHeight() - AndroidUtilities.dp(16);
                } else {
                    int h = AndroidUtilities.dp(140) + statusBarHeight;
                    if (backgroundView.getMeasuredHeight() + AndroidUtilities.dp(24) > h) {
                        h = backgroundView.getMeasuredHeight() + AndroidUtilities.dp(24);
                    }
                    firstViewHeight = h;
                }
                firstViewHeight -= 2.5f * yOffset;
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(firstViewHeight, MeasureSpec.EXACTLY));
            }
        };
        emptyLayout.setBackgroundColor(Theme.getColor(Theme.key_dialogBackgroundGray));

        super.createView(context);

        balanceView = new StarsBalanceView(context, currentAccount);
        actionBar.addView(balanceView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.BOTTOM));

//        yOffset = dp(16);
        aboveTitleView = new FrameLayout(context);
        aboveTitleView.setClickable(true);
        iconTextureView = new GLIconTextureView(context, GLIconRenderer.DIALOG_STYLE, Icon3D.TYPE_GOLDEN_STAR);
        iconTextureView.mRenderer.colorKey1 = Theme.key_starsGradient1;
        iconTextureView.mRenderer.colorKey2 = Theme.key_starsGradient2;
        iconTextureView.mRenderer.updateColors();
        iconTextureView.setStarParticlesView(particlesView);
        aboveTitleView.addView(iconTextureView, LayoutHelper.createFrame(190, 190, Gravity.CENTER, 0, 32, 0, 24));
        configureHeader(getString(R.string.TelegramStars), getString(R.string.TelegramStarsInfo), aboveTitleView, null);

        listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setSupportsChangeAnimations(false);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDurations(350);
        listView.setItemAnimator(itemAnimator);
        listView.setOnItemClickListener((view, position) -> {
            if (adapter == null) return;
            UItem item = adapter.getItem(position);
            if (item == null) return;
            onItemClick(item, position);
        });

        fireworksOverlay = new FireworksOverlay(getContext());
        contentView.addView(fireworksOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    @Override
    protected ContentView createContentView() {
        return new NestedFrameLayout(getContext());
    }

    private class NestedFrameLayout extends ContentView implements NestedScrollingParent3 {

        private NestedScrollingParentHelper nestedScrollingParentHelper;

        public NestedFrameLayout(Context context) {
            super(context);
            nestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, int[] consumed) {
            try {
                if (target == listView && transactionsLayout.isAttachedToWindow()) {
                    RecyclerListView innerListView = transactionsLayout.getCurrentListView();
                    int bottom = ((View) transactionsLayout.getParent()).getBottom();
                    if (listView.getHeight() - bottom >= 0) {
                        consumed[1] = dyUnconsumed;
                        innerListView.scrollBy(0, dyUnconsumed);
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        RecyclerListView innerListView = transactionsLayout.getCurrentListView();
                        if (innerListView != null && innerListView.getAdapter() != null) {
                            innerListView.getAdapter().notifyDataSetChanged();
                        }
                    } catch (Throwable e2) {

                    }
                });
            }
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {

        }

        @Override
        public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
            return super.onNestedPreFling(target, velocityX, velocityY);
        }

        @Override
        public void onNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
            if (target == listView && transactionsLayout.isAttachedToWindow()) {
                boolean searchVisible = actionBar.isSearchFieldVisible();
                int t = ((View) transactionsLayout.getParent()).getTop() - AndroidUtilities.statusBarHeight - ActionBar.getCurrentActionBarHeight();
                int bottom = ((View) transactionsLayout.getParent()).getBottom();
                if (dy < 0) {
                    boolean scrolledInner = false;
                    if (listView.getHeight() - bottom >= 0) {
                        RecyclerListView innerListView = transactionsLayout.getCurrentListView();
                        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) innerListView.getLayoutManager();
                        int pos = linearLayoutManager.findFirstVisibleItemPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            RecyclerView.ViewHolder holder = innerListView.findViewHolderForAdapterPosition(pos);
                            int top = holder != null ? holder.itemView.getTop() : -1;
                            int paddingTop = innerListView.getPaddingTop();
                            if (top != paddingTop || pos != 0) {
                                consumed[1] = pos != 0 ? dy : Math.max(dy, (top - paddingTop));
                                innerListView.scrollBy(0, dy);
                                scrolledInner = true;
                            }
                        }
                    }
                    if (searchVisible) {
                        if (!scrolledInner && t < 0) {
                            consumed[1] = dy - Math.max(t, dy);
                        } else {
                            consumed[1] = dy;
                        }
                    }
                } else {
                    if (searchVisible) {
                        RecyclerListView innerListView = transactionsLayout.getCurrentListView();
                        consumed[1] = dy;
                        if (t > 0) {
                            consumed[1] -= dy;
                        }
                        if (innerListView != null && consumed[1] > 0) {
                            innerListView.scrollBy(0, consumed[1]);
                        }
                    } else if (dy > 0) {
                        RecyclerListView innerListView = transactionsLayout.getCurrentListView();
                        if (listView.getHeight() - bottom >= 0 && innerListView != null && !innerListView.canScrollVertically(1)) {
                            consumed[1] = dy;
                            listView.stopScroll();
                        }
                    }
                }
            }
        }

        @Override
        public boolean onStartNestedScroll(View child, View target, int axes, int type) {
            return axes == ViewCompat.SCROLL_AXIS_VERTICAL;
        }

        @Override
        public void onNestedScrollAccepted(View child, View target, int axes, int type) {
            nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        }

        @Override
        public void onStopNestedScroll(View target, int type) {
            nestedScrollingParentHelper.onStopNestedScroll(target);
        }

        @Override
        public void onStopNestedScroll(View child) {

        }
    }

    public boolean attachedTransactionsLayout() {
        if (transactionsLayout == null || !(transactionsLayout.getParent() instanceof View))
            return false;
        int bottom = ((View) transactionsLayout.getParent()).getBottom();
        return listView.getHeight() - bottom >= 0;
    }

    @Override
    protected boolean drawActionBarShadow() {
        return !attachedTransactionsLayout();
    }

    @Override
    public StarParticlesView createParticlesView() {
        return makeParticlesView(getContext(), 75, 1);
    }

    public static StarParticlesView makeParticlesView(Context context, int particlesCount, int type) {
        return new StarParticlesView(context) {
            Paint[] paints;

            @Override
            protected void configure() {
                drawable = new Drawable(particlesCount);
                drawable.type = 105;
                drawable.roundEffect = false;
                drawable.useRotate = false;
                drawable.useBlur = true;
                drawable.checkBounds = true;
                drawable.isCircle = false;
                drawable.useScale = true;
                drawable.startFromCenter = true;
                if (type == 1) {
                    drawable.centerOffsetY = dp(32 - 8);
                }
                paints = new Paint[20];
                for (int i = 0; i < paints.length; ++i) {
                    paints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paints[i].setColorFilter(new PorterDuffColorFilter(ColorUtils.blendARGB(0xFFFA5416, 0xFFFFC837, i / (float) (paints.length - 1)), PorterDuff.Mode.SRC_IN));
                }
                drawable.getPaint = i -> paints[i % paints.length];
                drawable.size1 = 17;
                drawable.size2 = 18;
                drawable.size3 = 19;
                drawable.colorKey = Theme.key_windowBackgroundWhiteBlackText;
                drawable.init();
            }

            @Override
            protected int getStarsRectWidth() {
                return getMeasuredWidth();
            }

            { setClipWithGradient(); }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        if (iconTextureView != null) {
            iconTextureView.setPaused(false);
            iconTextureView.setDialogVisible(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (iconTextureView != null) {
            iconTextureView.setPaused(true);
            iconTextureView.setDialogVisible(true);
        }
    }

    @Override
    protected View getHeader(Context context) {
        return super.getHeader(context);
    }

    private UniversalAdapter adapter;
    @Override
    protected RecyclerView.Adapter<?> createAdapter() {
        return adapter = new UniversalAdapter(listView, getContext(), currentAccount, classGuid, true, this::fillItems, getResourceProvider()) {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                if (viewType == UniversalAdapter.VIEW_TYPE_ANIMATED_HEADER) {
                    HeaderCell headerCell = new HeaderCell(getContext(), Theme.key_windowBackgroundWhiteBlueHeader, 21, 0, false, resourceProvider);
                    headerCell.setHeight(40 - 15);
                    return new RecyclerListView.Holder(headerCell);
                }
                return super.onCreateViewHolder(parent, viewType);
            }
        };
    }

    private boolean expanded = false;
    private final int BUTTON_EXPAND = -1;
    private final int BUTTON_GIFT = -2;
    private final int BUTTON_SUBSCRIPTIONS_EXPAND = -3;

    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (getContext() == null) {
            return;
        }

        final StarsController c = StarsController.getInstance(currentAccount);

        items.add(UItem.asFullyCustom(getHeader(getContext())));
        items.add(UItem.asAnimatedHeader(-99, getString(R.string.TelegramStarsChoose)));

        int stars = 1;
        ArrayList<TLRPC.TL_starsTopupOption> options = c.getOptions();
        if (options != null && !options.isEmpty()) {
            int hidden = 0;
            for (int id = 0; id < options.size(); ++id) {
                TLRPC.TL_starsTopupOption option = options.get(id);
                if (option.extended && !expanded) {
                    hidden++;
                    continue;
                }
                items.add(StarTierView.Factory.asStarTier(id, stars++, option));
            }
            if (!expanded && hidden > 0) {
                items.add(ExpandView.Factory.asExpand(BUTTON_EXPAND, getString(expanded ? R.string.NotifyLessOptions : R.string.NotifyMoreOptions), !expanded).accent());
            }
        } else {
            items.add(UItem.asFlicker(FlickerLoadingView.STAR_TIER));
            items.add(UItem.asFlicker(FlickerLoadingView.STAR_TIER));
            items.add(UItem.asFlicker(FlickerLoadingView.STAR_TIER));
            items.add(UItem.asFlicker(FlickerLoadingView.STAR_TIER));
            items.add(UItem.asFlicker(FlickerLoadingView.STAR_TIER));
        }

        items.add(UItem.asShadow(AndroidUtilities.replaceSingleTag(getString(R.string.StarsTOS), () -> {
            Browser.openUrl(getContext(), getString(R.string.StarsTOSLink));
        })));

        if (getMessagesController().starsGiftsEnabled) {
            items.add(UItem.asButton(BUTTON_GIFT, R.drawable.menu_stars_gift, getString(R.string.TelegramStarsGift)).accent());
            items.add(UItem.asShadow(null));
        }

        if (c.hasSubscriptions()) {
            items.add(UItem.asHeader(getString(R.string.StarMySubscriptions)));
            for (int i = 0; i < c.subscriptions.size(); ++i) {
                items.add(StarsSubscriptionView.Factory.asSubscription(c.subscriptions.get(i)));
            }
            if (c.isLoadingSubscriptions()) {
                items.add(UItem.asFlicker(items.size(), FlickerLoadingView.STAR_SUBSCRIPTION));
            } else if (!c.didFullyLoadSubscriptions()) {
                items.add(UItem.asButton(BUTTON_SUBSCRIPTIONS_EXPAND, R.drawable.arrow_more, getString(R.string.StarMySubscriptionsExpand)).accent());
            }
            items.add(UItem.asShadow(null));
        }

        if (hadTransactions = c.hasTransactions()) {
            items.add(UItem.asFullscreenCustom(transactionsLayout, ActionBar.getCurrentActionBarHeight() + AndroidUtilities.statusBarHeight));
        } else {
            items.add(UItem.asCustom(emptyLayout));
        }
    }

    public void onItemClick(UItem item, int position) {
        if (item.id == BUTTON_EXPAND) {
            expanded = !expanded;
            adapter.update(true);
        } else if (item.id == BUTTON_GIFT) {
            StarsController.getInstance(currentAccount).getGiftOptions();
            UserSelectorBottomSheet.open(UserSelectorBottomSheet.TYPE_STARS, 0, BirthdayController.getInstance(currentAccount).getState());
        } else if (item.id == BUTTON_SUBSCRIPTIONS_EXPAND) {
            StarsController.getInstance(currentAccount).loadSubscriptions();
            adapter.update(true);
        } else if (item.instanceOf(StarTierView.Factory.class)) {
            if (item.object instanceof TLRPC.TL_starsTopupOption) {
                StarsController.getInstance(currentAccount).buy(getParentActivity(), (TLRPC.TL_starsTopupOption) item.object, (success, error) -> {
                    if (getContext() == null) return;
                    if (success) {
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.stars_topup, getString(R.string.StarsAcquired), AndroidUtilities.replaceTags(formatPluralString("StarsAcquiredInfo", (int) item.longValue))).show();
                        fireworksOverlay.start(true);
                        StarsController.getInstance(currentAccount).invalidateTransactions(true);
                    } else if (error != null) {
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.error, LocaleController.formatString(R.string.UnknownErrorCode, error)).show();
                    }
                });
            }
        } else if (item.instanceOf(StarsSubscriptionView.Factory.class)) {
            if (item.object instanceof TLRPC.StarsSubscription) {
                showSubscriptionSheet(getContext(), currentAccount, (TLRPC.StarsSubscription) item.object, getResourceProvider());
            }
        }
    }

    public static class StarsBalanceView extends LinearLayout implements NotificationCenter.NotificationCenterDelegate {

        private final int currentAccount;
        private final TextView headerTextView;
        private final AnimatedTextView amountTextView;

        public StarsBalanceView(Context context, int currentAccount) {
            super(context);

            this.currentAccount = currentAccount;
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);

            headerTextView = new TextView(context);
            headerTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            headerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            headerTextView.setText(getString(R.string.StarsBalance));
            headerTextView.setGravity(Gravity.RIGHT);
            headerTextView.setTypeface(AndroidUtilities.bold());
            addView(headerTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT));

            Drawable starDrawable = context.getResources().getDrawable(R.drawable.star_small_inner).mutate();
            amountTextView = new AnimatedTextView(context) {
                @Override
                protected void dispatchDraw(Canvas canvas) {
                    int x = (int) (getMeasuredWidth() - getDrawable().getCurrentWidth() - dp(20));
                    starDrawable.setBounds(x, (getMeasuredHeight() - dp(17)) / 2, x + dp(17), (getMeasuredHeight() + dp(17)) / 2);
                    starDrawable.draw(canvas);
                    super.dispatchDraw(canvas);
                }
            };
            amountTextView.adaptWidth = true;
            amountTextView.getDrawable().setHacks(false, true, true);
            amountTextView.setTypeface(AndroidUtilities.bold());
            amountTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            amountTextView.setTextSize(dp(13));
            amountTextView.setGravity(Gravity.RIGHT);
            amountTextView.setPadding(dp(19), 0, 0, 0);
            addView(amountTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 20, Gravity.RIGHT, 0, -2, 0, 0));

            updateBalance(false);

            setPadding(dp(15), dp(4), dp(15), dp(4));
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            updateBalance(false);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starBalanceUpdated);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starBalanceUpdated);
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.starBalanceUpdated) {
                updateBalance(true);
            }
        }

        private SpannableString loadingString;
        private long lastBalance = -1;

        public void updateBalance(boolean animated) {
            StarsController c = StarsController.getInstance(currentAccount);

            amountTextView.cancelAnimation();
            long balance = StarsController.getInstance(currentAccount).getBalance();
            if (balance > lastBalance && lastBalance != -1) {
                bounce();
            }
            if (!c.balanceAvailable()) {
                if (loadingString == null) {
                    loadingString = new SpannableString("x");
                    loadingString.setSpan(new LoadingSpan(amountTextView, dp(48)), 0, loadingString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                amountTextView.setText(loadingString, animated);
                lastBalance = -1;
            } else {
                amountTextView.setText(LocaleController.formatNumber(balance, ' '));
                lastBalance = balance;
            }
        }

        private ValueAnimator bounceAnimator;
        public void bounce() {
            if (bounceAnimator != null) {
                bounceAnimator.cancel();
            }
            bounceAnimator = ValueAnimator.ofFloat(.9f, 1f);
            bounceAnimator.addUpdateListener(anm -> {
                float t = (float) anm.getAnimatedValue();
                amountTextView.setScaleX(t);
                amountTextView.setScaleY(t);
            });
            bounceAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    amountTextView.setScaleX(1f);
                    amountTextView.setScaleY(1f);
                }
            });
            bounceAnimator.setDuration(320);
            bounceAnimator.setInterpolator(new OvershootInterpolator());
            bounceAnimator.start();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(ActionBar.getCurrentActionBarHeight(), MeasureSpec.EXACTLY));
        }
    }

    public static class StarTierView extends FrameLayout {

        private final Drawable starDrawableOutline;
        private final Drawable starDrawable;
        private final TextView textView;
        private final AnimatedTextView textView2;

        public StarTierView(@NonNull Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);

            starDrawableOutline = context.getResources().getDrawable(R.drawable.star_small_outline).mutate();
            starDrawableOutline.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogBackground, resourcesProvider), PorterDuff.Mode.SRC_IN));
            starDrawable = context.getResources().getDrawable(R.drawable.star_small_inner).mutate();
            setWillNotDraw(false);

            textView = new TextView(context);
            textView.setTypeface(AndroidUtilities.bold());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            addView(textView, LayoutHelper.createFrameRelatively(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.START, 48, 0, 0, 0));

            textView2 = new AnimatedTextView(context);
            textView2.setTextSize(dp(15));
            textView2.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
            textView2.setGravity(LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT);
            addView(textView2, LayoutHelper.createFrameRelatively(LayoutHelper.WRAP_CONTENT, 21, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, 19, 0));
        }

        private SpannableString loading;

        private boolean needDivider;
        public void set(int starsCount, CharSequence text1, CharSequence text2, boolean divider) {
            final boolean animated = TextUtils.equals(textView.getText(), text1);

            this.starsCount = starsCount;
            if (!animated) {
                animatedStarsCount.set(starsCount, true);
            }
            textView.setText(text1);
            if (text2 == null) {
                if (loading == null) {
                    loading = new SpannableString("x");
                    loading.setSpan(new LoadingSpan(textView2, dp(55)), 0, loading.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                text2 = loading;
            }
            textView2.setText(text2);
            final float rtl = LocaleController.isRTL ? -1f : 1f;
            if (animated) {
                textView.animate().translationX(rtl * (starsCount - 1) * dp(2.66f)).setDuration(320).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).start();
            } else {
                textView.setTranslationX(rtl * (starsCount - 1) * dp(2.66f));
            }

            needDivider = divider;
            invalidate();
        }

        private int starsCount;
        private final AnimatedFloat animatedStarsCount = new AnimatedFloat(this, 0, 500, CubicBezierInterpolator.EASE_OUT_QUINT);

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            final float starsCount = animatedStarsCount.set(this.starsCount);
            final float rtl = LocaleController.isRTL ? -1f : 1f;
            final float wsize = dp(24), hsize = dp(24);
            final float pad = dp(2.5f);
            final float sx = LocaleController.isRTL ? getWidth() - dp(19) - wsize : dp(19);
            for (int i = (int) Math.ceil(starsCount) - 1; i >= 0; --i) {
                final float alpha = Utilities.clamp(starsCount - i, 1f, 0f);
                final float x = sx + (i - 1 - (1f - alpha)) * pad * rtl;
                final float y = (getMeasuredHeight() - hsize) / 2f;
                starDrawableOutline.setBounds((int) x, (int) y, (int) (x + wsize), (int) (y + hsize));
                starDrawableOutline.setAlpha((int) (0xFF * alpha));
                starDrawableOutline.draw(canvas);
                starDrawable.setBounds((int) x, (int) y, (int) (x + wsize), (int) (y + hsize));
                starDrawable.setAlpha((int) (0xFF * alpha));
                starDrawable.draw(canvas);
            }

            if (needDivider) {
                canvas.drawRect(LocaleController.isRTL ? 0 : dp(22), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? dp(22) : 0), getMeasuredHeight(), Theme.dividerPaint);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(48), MeasureSpec.EXACTLY)
            );
        }

        public static class Factory extends UItem.UItemFactory<StarTierView> {
            @Override
            public StarTierView createView(Context context, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
                return new StarTierView(context, resourcesProvider);
            }

            @Override
            public void bindView(View view, UItem item, boolean divider) {
                ((StarTierView) view).set(item.intValue, item.text, item.subtext, divider);
            }

            public static UItem asStarTier(int id, int index, TLRPC.TL_starsTopupOption option) {
                UItem item = UItem.ofFactory(StarTierView.Factory.class);
                item.id = id;
                item.intValue = index;
                item.longValue = option.stars;
                item.text = formatPluralStringComma("StarsCount", (int) option.stars, ' ');
                item.subtext = option.loadingStorePrice ? null : BillingController.getInstance().formatCurrency(option.amount, option.currency);
                item.object = option;
                return item;
            }

            public static UItem asStarTier(int id, int index, TLRPC.TL_starsGiftOption option) {
                UItem item = UItem.ofFactory(StarTierView.Factory.class);
                item.id = id;
                item.intValue = index;
                item.longValue = option.stars;
                item.text = formatPluralStringComma("StarsCount", (int) option.stars, ' ');
                item.subtext = option.loadingStorePrice ? null : BillingController.getInstance().formatCurrency(option.amount, option.currency);
                item.object = option;
                return item;
            }

            @Override
            public boolean equals(UItem a, UItem b) {
                return a.id == b.id;
            }

            @Override
            public boolean contentsEquals(UItem a, UItem b) {
                return a.intValue == b.intValue && a.id == b.id && TextUtils.equals(a.subtext, b.subtext);
            }
        }
    }

    public static class ExpandView extends FrameLayout {

        public final AnimatedTextView textView;
        public final ImageView arrowView;

        public ExpandView(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);

            textView = new AnimatedTextView(context);
            textView.getDrawable().setHacks(true, true, true);
            textView.setTextSize(dp(15));
            addView(textView, LayoutHelper.createFrameRelatively(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL | Gravity.START, 22, 0, 17 + 24 + 17, 0));

            arrowView = new ImageView(context);
            arrowView.setScaleType(ImageView.ScaleType.CENTER);
            arrowView.setImageResource(R.drawable.arrow_more);
            addView(arrowView, LayoutHelper.createFrameRelatively(24, 24, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, 17, 0));
        }

        private int lastId;
        private boolean needDivider;
        public void set(UItem item, boolean divider) {
            final boolean animated = lastId == item.id;
            lastId = item.id;
            textView.setText(item.text, animated);
            final int color = Theme.getColor(item.accent ? Theme.key_windowBackgroundWhiteBlueText2 : Theme.key_windowBackgroundWhiteBlackText);
            textView.setTextColor(color);
            arrowView.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            if (animated) {
                arrowView.animate().rotation(item.collapsed ? 0 : 180).setDuration(340).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            } else {
                arrowView.setRotation(item.collapsed ? 0 : 180);
            }
            setWillNotDraw(!(needDivider = divider));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (needDivider) {
                canvas.drawRect(LocaleController.isRTL ? 0 : dp(22), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? dp(22) : 0), getMeasuredHeight(), Theme.dividerPaint);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(dp(48), MeasureSpec.EXACTLY)
            );
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            lastId = Integer.MAX_VALUE;
        }

        public static class Factory extends UItem.UItemFactory<ExpandView> {
            @Override
            public ExpandView createView(Context context, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
                return new ExpandView(context, resourcesProvider);
            }

            @Override
            public void bindView(View view, UItem item, boolean divider) {
                ((ExpandView) view).set(item, divider);
            }

            public static UItem asExpand(int id, CharSequence text, boolean collapsed) {
                UItem item = UItem.ofFactory(ExpandView.Factory.class);
                item.id = id;
                item.text = text;
                item.collapsed = collapsed;
                return item;
            }
        }
    }

    public static class StarsTransactionsLayout extends LinearLayout implements NotificationCenter.NotificationCenterDelegate {

        private final int currentAccount;
        private final ViewPagerFixed viewPager;
        private final PageAdapter adapter;
        private final ViewPagerFixed.TabsView tabsView;
        private final long bot_id;

        private static class PageAdapter extends ViewPagerFixed.Adapter {

            private final Context context;
            private final int currentAccount;
            private final int classGuid;
            private final Theme.ResourcesProvider resourcesProvider;
            private final long bot_id;

            public PageAdapter(Context context, int currentAccount, long bot_id, int classGuid, Theme.ResourcesProvider resourcesProvider) {
                this.context = context;
                this.currentAccount = currentAccount;
                this.classGuid = classGuid;
                this.resourcesProvider = resourcesProvider;
                this.bot_id = bot_id;
                fill();
            }

            private final ArrayList<UItem> items = new ArrayList<>();

            public void fill() {
                items.clear();
                if (bot_id == 0) {
                    StarsController s = StarsController.getInstance(currentAccount);
                    items.add(UItem.asSpace(StarsController.ALL_TRANSACTIONS));
                    if (s.hasTransactions(StarsController.INCOMING_TRANSACTIONS)) {
                        items.add(UItem.asSpace(StarsController.INCOMING_TRANSACTIONS));
                    }
                    if (s.hasTransactions(StarsController.OUTGOING_TRANSACTIONS)) {
                        items.add(UItem.asSpace(StarsController.OUTGOING_TRANSACTIONS));
                    }
                } else {
                    BotStarsController s = BotStarsController.getInstance(currentAccount);
                    items.add(UItem.asSpace(StarsController.ALL_TRANSACTIONS));
                    if (s.hasTransactions(bot_id, BotStarsController.INCOMING_TRANSACTIONS)) {
                        items.add(UItem.asSpace(BotStarsController.INCOMING_TRANSACTIONS));
                    }
                    if (s.hasTransactions(bot_id, BotStarsController.OUTGOING_TRANSACTIONS)) {
                        items.add(UItem.asSpace(BotStarsController.OUTGOING_TRANSACTIONS));
                    }
                }
            }

            @Override
            public int getItemCount() {
                return items.size();
            }

            @Override
            public View createView(int viewType) {
                return new Page(context, bot_id, viewType, currentAccount, classGuid, resourcesProvider);
            }

            @Override
            public void bindView(View view, int position, int viewType) {}

            @Override
            public int getItemViewType(int position) {
                if (position < 0 || position >= items.size())
                    return StarsController.ALL_TRANSACTIONS;
                return items.get(position).intValue;
            }

            @Override
            public String getItemTitle(int position) {
                final int viewType = getItemViewType(position);
                switch (viewType) {
                    case StarsController.ALL_TRANSACTIONS: return getString(R.string.StarsTransactionsAll);
                    case StarsController.INCOMING_TRANSACTIONS: return getString(R.string.StarsTransactionsIncoming);
                    case StarsController.OUTGOING_TRANSACTIONS: return getString(R.string.StarsTransactionsOutgoing);
                    default: return "";
                }
            }
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.starTransactionsLoaded) {
                adapter.fill();
                viewPager.fillTabs(true);
            }
        }

        public StarsTransactionsLayout(Context context, int currentAccount, long bot_id, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.currentAccount = currentAccount;
            this.bot_id = bot_id;

            setOrientation(VERTICAL);

            viewPager = new ViewPagerFixed(context);
            viewPager.setAdapter(adapter = new PageAdapter(context, currentAccount, bot_id, classGuid, resourcesProvider));
            tabsView = viewPager.createTabsView(true, 3);

            View separatorView = new View(context);
            separatorView.setBackgroundColor(Theme.getColor(Theme.key_divider, resourcesProvider));

            addView(tabsView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
            addView(separatorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1f / AndroidUtilities.density));
            addView(viewPager, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            setBackgroundColor(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));
        }

        @Override
        protected void onAttachedToWindow() {
            adapter.fill();
            viewPager.fillTabs(false);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starTransactionsLoaded);
            super.onAttachedToWindow();
        }

        @Override
        protected void onDetachedFromWindow() {
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starTransactionsLoaded);
            super.onDetachedFromWindow();
        }

        public RecyclerListView getCurrentListView() {
            View currentView = viewPager.getCurrentView();
            if (!(currentView instanceof Page)) return null;
            return ((Page) currentView).listView;
        }

        public static class Page extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

            private final UniversalRecyclerView listView;
            private final Theme.ResourcesProvider resourcesProvider;
            private final int currentAccount;
            private final int type;
            private final long bot_id;

            public Page(Context context, long bot_id, int type, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
                super(context);

                this.type = type;
                this.currentAccount = currentAccount;
                this.bot_id = bot_id;
                this.resourcesProvider = resourcesProvider;

                loadTransactionsRunnable = () -> {
                    if (bot_id != 0) {
                        BotStarsController.getInstance(currentAccount).loadTransactions(bot_id, type);
                    } else {
                        StarsController.getInstance(currentAccount).loadTransactions(type);
                    }
                };

                listView = new UniversalRecyclerView(context, currentAccount, classGuid, true, this::fillItems, this::onClick, null, resourcesProvider);
                addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
                listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        if (!Page.this.listView.canScrollVertically(1) || isLoadingVisible()) {
                            loadTransactionsRunnable.run();
                        }
                    }
                });
            }

            private final Runnable loadTransactionsRunnable;

            public boolean isLoadingVisible() {
                for (int i = 0; i < listView.getChildCount(); ++i) {
                    if (listView.getChildAt(i) instanceof FlickerLoadingView)
                        return true;
                }
                return false;
            }

            @Override
            public void didReceivedNotification(int id, int account, Object... args) {
                if (id == NotificationCenter.starTransactionsLoaded) {
                    listView.adapter.update(true);
                    loadTransactionsRunnable.run();
                } else if (id == NotificationCenter.botStarsTransactionsLoaded) {
                    if ((long) args[0] == bot_id) {
                        listView.adapter.update(true);
                    }
                }
            }

            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                if (bot_id != 0) {
                    NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.botStarsTransactionsLoaded);
                } else {
                    NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starTransactionsLoaded);
                }
                listView.adapter.update(false);
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                if (bot_id != 0) {
                    NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.botStarsTransactionsLoaded);
                } else {
                    NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starTransactionsLoaded);
                }
            }

            private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
                if (bot_id != 0) {
                    final BotStarsController c = BotStarsController.getInstance(currentAccount);
                    for (TLRPC.StarsTransaction t : c.getTransactions(bot_id, type)) {
                        items.add(StarsTransactionView.Factory.asTransaction(t, true));
                    }
                    if (!c.didFullyLoadTransactions(bot_id, type)) {
                        items.add(UItem.asFlicker(items.size(), FlickerLoadingView.DIALOG_CELL_TYPE));
                        items.add(UItem.asFlicker(items.size(), FlickerLoadingView.DIALOG_CELL_TYPE));
                        items.add(UItem.asFlicker(items.size(), FlickerLoadingView.DIALOG_CELL_TYPE));
                    }
                } else {
                    final StarsController c = StarsController.getInstance(currentAccount);
                    for (TLRPC.StarsTransaction t : c.transactions[type]) {
                        items.add(StarsTransactionView.Factory.asTransaction(t, false));
                    }
                    if (!c.didFullyLoadTransactions(type)) {
                        items.add(UItem.asFlicker(items.size(), FlickerLoadingView.DIALOG_CELL_TYPE));
                        items.add(UItem.asFlicker(items.size(), FlickerLoadingView.DIALOG_CELL_TYPE));
                        items.add(UItem.asFlicker(items.size(), FlickerLoadingView.DIALOG_CELL_TYPE));
                    }
                }
            }

            private void onClick(UItem item, View view, int position, float x, float y) {
                if (item.object instanceof TLRPC.StarsTransaction) {
                    showTransactionSheet(getContext(), false, 0, currentAccount, (TLRPC.StarsTransaction) item.object, resourcesProvider);
                }
            }

        }
    }

    public static class Transaction {
        public int date;
        public String platform;
        public long userId;
        public int stars;

        public static Transaction as(int date, String platform, int stars) {
            Transaction t = new Transaction();
            t.date = date;
            t.platform = platform;
            t.stars = stars;
            return t;
        }

        public static Transaction as(int date, long userId, int stars) {
            Transaction t = new Transaction();
            t.date = date;
            t.userId = userId;
            t.stars = stars;
            return t;
        }
    }

    public static class StarsTransactionView extends LinearLayout {

        private final int currentAccount;

        private final AvatarDrawable avatarDrawable;
        private final FrameLayout imageViewContainer;
        private final BackupImageView imageView;
        private final BackupImageView imageView2;
        private int imageViewCount = 1;
        private final LinearLayout textLayout;
        private final TextView titleTextView;
        private final LinearLayout.LayoutParams titleTextViewParams;
        private final TextView subtitleTextView;
        private final TextView dateTextView;
        private final LinearLayout.LayoutParams dateTextViewParams;
        private final TextView amountTextView;

        private final SpannableString star;

        private boolean threeLines;

        public StarsTransactionView(Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.currentAccount = currentAccount;

            setOrientation(HORIZONTAL);

            imageViewContainer = new FrameLayout(context) {
                private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                @Override
                protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                    if (imageViewCount > 1) {
                        backgroundPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
                        AndroidUtilities.rectTmp.set(child.getX(), child.getY(), child.getX() + child.getWidth(), child.getY() + child.getHeight());
                        AndroidUtilities.rectTmp.inset(-dp(1.66f), -dp(1.66f));
                        canvas.drawRoundRect(AndroidUtilities.rectTmp, dp(13), dp(13), backgroundPaint);
                    }
                    return super.drawChild(canvas, child, drawingTime);
                }
            };
            addView(imageViewContainer, LayoutHelper.createLinear(72, LayoutHelper.MATCH_PARENT, 0, Gravity.LEFT | Gravity.FILL_VERTICAL));

            imageView2 = new BackupImageView(context);
            imageView2.setRoundRadius(dp(46));
            imageViewContainer.addView(imageView2, LayoutHelper.createFrame(46, 46, Gravity.CENTER_VERTICAL, 13, 0, 13, 0));

            avatarDrawable = new AvatarDrawable();
            imageView = new BackupImageView(context);
            imageView.setRoundRadius(dp(46));
            imageViewContainer.addView(imageView, LayoutHelper.createFrame(46, 46, Gravity.CENTER_VERTICAL, 13, 0, 13, 0));

            textLayout = new LinearLayout(context);
            textLayout.setOrientation(VERTICAL);
            textLayout.setGravity(Gravity.CENTER | Gravity.LEFT);
            addView(textLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, 1, Gravity.FILL));

            titleTextView = new TextView(context);
            titleTextView.setTypeface(AndroidUtilities.bold());
            titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            titleTextView.setEllipsize(TextUtils.TruncateAt.END);
            titleTextView.setSingleLine(true);
            textLayout.addView(titleTextView, titleTextViewParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4.33f));

            subtitleTextView = new TextView(context);
            subtitleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subtitleTextView.setEllipsize(TextUtils.TruncateAt.END);
            subtitleTextView.setSingleLine(true);
            textLayout.addView(subtitleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, .33f));

            dateTextView = new TextView(context);
            dateTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
            dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            dateTextView.setEllipsize(TextUtils.TruncateAt.END);
            dateTextView.setSingleLine(true);
            textLayout.addView(dateTextView, dateTextViewParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            amountTextView = new TextView(context);
            amountTextView.setTypeface(AndroidUtilities.bold());
            amountTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.3f);
            amountTextView.setGravity(Gravity.RIGHT);
            addView(amountTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, Gravity.CENTER_VERTICAL | Gravity.RIGHT, 8, 0, 20, 0));

            star = new SpannableString("⭐️");
            Drawable drawable = context.getResources().getDrawable(R.drawable.star_small_inner).mutate();
            drawable.setBounds(0, 0, dp(21), dp(21));
            star.setSpan(new ImageSpan(drawable), 0, star.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        public static HashMap<String, CombinedDrawable> cachedPlatformDrawables;
        public static CombinedDrawable getPlatformDrawable(String platform) {
            return getPlatformDrawable(platform, 44);
        }
        public static CombinedDrawable getPlatformDrawable(String platform, int sz) {
            if (sz != 44) return SessionCell.createDrawable(sz, platform);
            if (cachedPlatformDrawables == null) {
                cachedPlatformDrawables = new HashMap<>();
            }
            CombinedDrawable drawable = cachedPlatformDrawables.get(platform);
            if (drawable == null) {
                cachedPlatformDrawables.put(platform, drawable = SessionCell.createDrawable(44, platform));
            }
            return drawable;
        }

        private boolean needDivider;
        public void set(TLRPC.StarsTransaction transaction, boolean bot, boolean divider) {
            long did = DialogObject.getPeerDialogId(transaction.peer.peer);

            threeLines = did != 0 || transaction.subscription || transaction.gift && transaction.peer instanceof TLRPC.TL_starsTransactionPeerFragment;
            titleTextViewParams.bottomMargin = threeLines ? 0 : dp(4.33f);
            subtitleTextView.setVisibility(threeLines ? View.VISIBLE : View.GONE);
            dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, threeLines ? 13 : 14);

            dateTextView.setText(LocaleController.formatShortDateTime(transaction.date));
            if (transaction.refund) {
                dateTextView.setText(TextUtils.concat(dateTextView.getText(), " — ", getString(R.string.StarsRefunded)));
            } else if (transaction.failed) {
                dateTextView.setText(TextUtils.concat(dateTextView.getText(), " — ", getString(R.string.StarsFailed)));
            } else if (transaction.pending) {
                dateTextView.setText(TextUtils.concat(dateTextView.getText(), " — ", getString(R.string.StarsPending)));
            }

            imageView.setTranslationX(0);
            imageView.setTranslationY(0);
            imageView2.setVisibility(GONE);
            imageView.setRoundRadius(dp(46));
            if (did != 0) {
                boolean deleted = false;
                String username;
                if (UserObject.isService(did)) {
                    username = getString(R.string.StarsTransactionUnknown);
                    imageView.setImageDrawable(getPlatformDrawable("fragment"));
                } else if (did >= 0) {
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(did);
                    deleted = user == null;
                    if (transaction.photo == null) {
                        avatarDrawable.setInfo(user);
                        imageView.setForUserOrChat(user, avatarDrawable);
                    }
                    username = UserObject.getUserName(user);
                } else {
                    TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
                    deleted = chat == null;
                    if (transaction.photo == null) {
                        avatarDrawable.setInfo(chat);
                        imageView.setForUserOrChat(chat, avatarDrawable);
                    }
                    username = chat == null ? "" : chat.title;
                }
                if (transaction.subscription) {
                    titleTextView.setText(username);
                    if (transaction.subscription_period == StarsController.PERIOD_MONTHLY) {
                        subtitleTextView.setVisibility(VISIBLE);
                        subtitleTextView.setText(getString(R.string.StarsTransactionSubscriptionMonthly));
                    } else {
                        final String period = transaction.subscription_period == StarsController.PERIOD_5MINUTES ? "5 minutes" : "Minute";
                        subtitleTextView.setVisibility(VISIBLE);
                        subtitleTextView.setText(String.format(Locale.US, "%s subscription fee", period));
                    }
                } else if (transaction.gift) {
                    titleTextView.setText(LocaleController.getString(R.string.StarsGiftReceived));
                    subtitleTextView.setVisibility(deleted ? GONE : VISIBLE);
                    subtitleTextView.setText(username);
                } else if (transaction.reaction) {
                    titleTextView.setText(LocaleController.getString(R.string.StarsReactionsSent));
                    subtitleTextView.setVisibility(deleted ? GONE : VISIBLE);
                    subtitleTextView.setText(username);
                } else if (!transaction.extended_media.isEmpty()) {
                    if (bot) {
                        titleTextView.setText(username);
                        subtitleTextView.setVisibility(VISIBLE);
                        subtitleTextView.setText(LocaleController.getString(R.string.StarMediaPurchase));
                    } else {
                        titleTextView.setText(LocaleController.getString(R.string.StarMediaPurchase));
                        subtitleTextView.setVisibility(deleted ? GONE : VISIBLE);
                        subtitleTextView.setText(username);
                    }
                    imageViewCount = 0;
                    for (int i = 0; i < Math.min(2, transaction.extended_media.size()); ++i) {
                        TLRPC.MessageMedia media = transaction.extended_media.get(i);
                        BackupImageView imageView = i == 0 ? this.imageView : this.imageView2;

                        imageView.setRoundRadius(dp(12));
                        ImageLocation location = null;
                        if (media instanceof TLRPC.TL_messageMediaPhoto) {
                            location = ImageLocation.getForPhoto(FileLoader.getClosestPhotoSizeWithSize(media.photo.sizes, dp(46), true), media.photo);
                        } else if (media instanceof TLRPC.TL_messageMediaDocument) {
                            location = ImageLocation.getForDocument(FileLoader.getClosestPhotoSizeWithSize(media.document.thumbs, dp(46), true), media.document);
                        }
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImage(location, "46_46", null, null, null, 0);
                        imageViewCount++;
                    }
                    for (int i = 0; i < imageViewCount; ++i) {
                        BackupImageView imageView = i == 0 ? this.imageView : this.imageView2;
                        imageView.setTranslationX(dp(2) + (i - imageViewCount / 2f) * dp(4.33f));
                        imageView.setTranslationY((i - imageViewCount / 2f) * dp(4.33f));
                    }
                } else if (transaction.photo != null) {
                    titleTextView.setText(transaction.title != null ? transaction.title : "");
                    subtitleTextView.setVisibility(deleted ? GONE : VISIBLE);
                    subtitleTextView.setText(username);
                    imageView.setImage(ImageLocation.getForWebFile(WebFile.createWithWebDocument(transaction.photo)), "46_46", null, 0, null);
                } else {
                    titleTextView.setText(transaction.title != null ? transaction.title : "");
                    subtitleTextView.setVisibility(deleted ? GONE : VISIBLE);
                    subtitleTextView.setText(username);
                }
            } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerAppStore) {
                titleTextView.setText(getString(R.string.StarsTransactionInApp));
                imageView.setImageDrawable(getPlatformDrawable("ios"));
            } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerPlayMarket) {
                titleTextView.setText(getString(R.string.StarsTransactionInApp));
                imageView.setImageDrawable(getPlatformDrawable("android"));
            } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerFragment) {
                if (transaction.gift) {
                    titleTextView.setText(LocaleController.getString(R.string.StarsGiftReceived));
                    subtitleTextView.setText(getString(R.string.StarsTransactionUnknown));
                    subtitleTextView.setVisibility(VISIBLE);
                } else {
                    titleTextView.setText(getString(bot ? R.string.StarsTransactionWithdrawFragment : R.string.StarsTransactionFragment));
                }
                imageView.setImageDrawable(getPlatformDrawable("fragment"));
            } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerPremiumBot) {
                titleTextView.setText(getString(R.string.StarsTransactionBot));
                imageView.setImageDrawable(getPlatformDrawable("premiumbot"));
            } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerUnsupported) {
                titleTextView.setText(getString(R.string.StarsTransactionUnsupported));
                imageView.setImageDrawable(getPlatformDrawable("?"));
            } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerAds) {
                titleTextView.setText(getString(R.string.StarsTransactionAds));
                imageView.setImageDrawable(getPlatformDrawable("ads"));
            } else {
                titleTextView.setText("");
                imageView.setImageDrawable(null);
            }

            if (transaction.stars > 0) {
                amountTextView.setVisibility(View.VISIBLE);
                amountTextView.setTextColor(Theme.getColor(Theme.key_color_green));
                amountTextView.setText(TextUtils.concat("+", LocaleController.formatNumber(transaction.stars, ' '), " ", star));
            } else if (transaction.stars < 0) {
                amountTextView.setVisibility(View.VISIBLE);
                amountTextView.setTextColor(Theme.getColor(Theme.key_color_red));
                amountTextView.setText(TextUtils.concat("-", LocaleController.formatNumber(-transaction.stars, ' '), " ", star));
            } else {
                amountTextView.setVisibility(View.GONE);
            }

            setWillNotDraw(!(needDivider = divider));
        }

        public void setLoading() {

        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (needDivider) {
                canvas.drawRect(LocaleController.isRTL ? 0 : dp(72), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? dp(72) : 0), getMeasuredHeight(), Theme.dividerPaint);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(threeLines ? 71 : 58), MeasureSpec.EXACTLY)
            );
        }

        public static class Factory extends UItem.UItemFactory<StarsTransactionView> {
            @Override
            public StarsTransactionView createView(Context context, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
                StarsTransactionView cached = getCached();
                if (cached != null) {
                    return cached;
                }
                return new StarsTransactionView(context, currentAccount, resourcesProvider);
            }

            @Override
            public void bindView(View view, UItem item, boolean divider) {
                StarsTransactionView transactionView = (StarsTransactionView) view;
                transactionView.set((TLRPC.StarsTransaction) item.object, item.accent, divider);
            }

            public static UItem asTransaction(TLRPC.StarsTransaction transaction, boolean bot) {
                UItem item = UItem.ofFactory(StarsTransactionView.Factory.class);
                item.object = transaction;
                item.accent = bot;
                return item;
            }

            public static UItem asLoading() {
                UItem item = UItem.ofFactory(StarsTransactionView.Factory.class);
                item.accent = true;
                return item;
            }
        }
    }

    public static class StarsSubscriptionView extends LinearLayout {

        private final int currentAccount;
        private final Theme.ResourcesProvider resourcesProvider;

        public final BackupImageView imageView;
        public final LinearLayout textLayout;
        public final SimpleTextView titleView;
        public final TextView subtitleView;
        public final LinearLayout priceLayout;
        public final TextView priceTitleView;
        public final TextView priceSubtitleView;

        private boolean needDivider;

        public StarsSubscriptionView(Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider) {
            super(context);

            this.currentAccount = currentAccount;
            this.resourcesProvider = resourcesProvider;
            setOrientation(HORIZONTAL);

            imageView = new BackupImageView(context);
            imageView.setRoundRadius(dp(46));
            addView(imageView, LayoutHelper.createLinear(46, 46, 0, Gravity.CENTER_VERTICAL | Gravity.LEFT, 13, 0, 13, 0));

            textLayout = new LinearLayout(context);
            textLayout.setOrientation(VERTICAL);
            addView(textLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

            titleView = new SimpleTextView(context);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            titleView.setTextSize(16);
            titleView.setTypeface(AndroidUtilities.bold());
            textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

            subtitleView = new TextView(context);
            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            textLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));

            priceLayout = new LinearLayout(context);
            priceLayout.setOrientation(VERTICAL);
            addView(priceLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, Gravity.CENTER_VERTICAL, 0, 0, 18, 0));

            priceTitleView = new TextView(context);
            priceTitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            priceTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            priceTitleView.setTypeface(AndroidUtilities.bold());
            priceTitleView.setGravity(Gravity.RIGHT);
            priceLayout.addView(priceTitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT, 0, 0, 0, 1));

            priceSubtitleView = new TextView(context);
            priceSubtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
            priceSubtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            priceSubtitleView.setGravity(Gravity.RIGHT);
            priceLayout.addView(priceSubtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT, 0, 0, 0, 0));
        }

        public void set(TLRPC.StarsSubscription subscription, boolean divider) {
            long dialogId = DialogObject.getPeerDialogId(subscription.peer);

            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat == null) return;

            AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setInfo(chat);
            imageView.setForUserOrChat(chat, avatarDrawable);

            long now = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
            titleView.setText(chat.title);
            if (subscription.canceled) {
                subtitleView.setText(formatString(subscription.until_date < now ? R.string.StarsSubscriptionExpired : R.string.StarsSubscriptionExpires, LocaleController.formatDateChat(subscription.until_date)));
                priceTitleView.setVisibility(View.GONE);
                priceSubtitleView.setTextColor(Theme.getColor(Theme.key_color_red, resourcesProvider));
                priceSubtitleView.setText(LocaleController.getString(R.string.StarsSubscriptionStatusCancelled));
            } else if (subscription.until_date < now) {
                subtitleView.setText(formatString(R.string.StarsSubscriptionExpired, LocaleController.formatDateChat(subscription.until_date)));
                priceTitleView.setVisibility(View.GONE);
                priceSubtitleView.setTextColor(Theme.getColor(Theme.key_color_red, resourcesProvider));
                priceSubtitleView.setText(LocaleController.getString(R.string.StarsSubscriptionStatusExpired));
            } else {
                subtitleView.setText(formatString(R.string.StarsSubscriptionRenews, LocaleController.formatDateChat(subscription.until_date)));
                priceTitleView.setVisibility(View.VISIBLE);
                priceTitleView.setText(replaceStarsWithPlain("⭐️ " + Long.toString(subscription.pricing.amount), .8f));
                priceSubtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
                if (subscription.pricing.period == StarsController.PERIOD_MONTHLY) {
                    priceSubtitleView.setText(getString(R.string.StarsParticipantSubscriptionPerMonth));
                } else if (subscription.pricing.period == StarsController.PERIOD_MINUTE) {
                    priceSubtitleView.setText("per minute");
                } else if (subscription.pricing.period == StarsController.PERIOD_5MINUTES) {
                    priceSubtitleView.setText("per 5 minutes");
                }
            }

            setWillNotDraw(!(needDivider = divider));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(58), MeasureSpec.EXACTLY)
            );
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (needDivider) {
                canvas.drawRect(dp(72), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight(), Theme.dividerPaint);
            }
        }

        public static class Factory extends UItem.UItemFactory<StarsSubscriptionView> {
            @Override
            public StarsSubscriptionView createView(Context context, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
                StarsSubscriptionView cached = getCached();
                if (cached != null) {
                    return cached;
                }
                return new StarsSubscriptionView(context, currentAccount, resourcesProvider);
            }

            @Override
            public void bindView(View view, UItem item, boolean divider) {
                StarsSubscriptionView subscriptionView = (StarsSubscriptionView) view;
                subscriptionView.set((TLRPC.StarsSubscription) item.object, divider);
            }

            public static UItem asSubscription(TLRPC.StarsSubscription subscription) {
                UItem item = UItem.ofFactory(StarsSubscriptionView.Factory.class);
                item.object = subscription;
                return item;
            }

            @Override
            public boolean equals(UItem a, UItem b) {
                if (a == null && b == null) return true;
                if (a == null || b == null) return false;
                if (!(a.object instanceof TLRPC.StarsSubscription) || !(b.object instanceof TLRPC.StarsSubscription)) return false;
                TLRPC.StarsSubscription subA = (TLRPC.StarsSubscription) a.object;
                TLRPC.StarsSubscription subB = (TLRPC.StarsSubscription) b.object;
                return TextUtils.equals(subA.id, subB.id);
            }
        }
    }

    public static BottomSheet openConfirmPurchaseSheet(
        Context context,
        Theme.ResourcesProvider resourcesProvider,
        int currentAccount,
        MessageObject messageObject,
        long userId,
        String purchase,
        long stars,
        TLRPC.WebDocument photo,
        Utilities.Callback<Utilities.Callback<Boolean>> whenConfirmed,
        Runnable whenDismissed
    ) {
        BottomSheet.Builder b = new BottomSheet.Builder(context, false, resourcesProvider);

        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(dp(16), 0, dp(16), dp(8));

        FrameLayout topView = new FrameLayout(context);
        topView.addView(makeParticlesView(context, 40, 0), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        if (messageObject != null && messageObject.messageOwner != null && messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPaidMedia) {
            BackupImageView imageView = new BackupImageView(context) {
                private SpoilerEffect2 spoilerEffect2;
                private Path clipPath = new Path();
                private RectF clipRect = new RectF();
                private Drawable lock = context.getResources().getDrawable(R.drawable.large_locked_post).mutate();
                @Override
                protected void dispatchDraw(Canvas canvas) {
                    super.dispatchDraw(canvas);
                    if (spoilerEffect2 == null) {
                        spoilerEffect2 = SpoilerEffect2.getInstance(this);
                    }
                    if (spoilerEffect2 != null) {
                        clipRect.set(0,0,getWidth(),getHeight());
                        clipPath.rewind();
                        clipPath.addRoundRect(clipRect, dp(24), dp(24), Path.Direction.CW);
                        canvas.save();
                        canvas.clipPath(clipPath);
                        spoilerEffect2.draw(canvas, this, getWidth(), getHeight(), 1f);
                        canvas.restore();
                    }
                    lock.setBounds((getWidth()-lock.getIntrinsicWidth())/2, (getHeight()-lock.getIntrinsicHeight())/2, (getWidth()+lock.getIntrinsicWidth())/2, (getHeight()+lock.getIntrinsicHeight())/2);
                    lock.draw(canvas);
                }
                @Override
                protected void onAttachedToWindow() {
                    if (spoilerEffect2 != null) {
                        spoilerEffect2.attach(this);
                    }
                    super.onAttachedToWindow();
                }
                @Override
                protected void onDetachedFromWindow() {
                    if (spoilerEffect2 != null) {
                        spoilerEffect2.detach(this);
                    }
                    super.onDetachedFromWindow();
                }
            };
            imageView.setRoundRadius(dp(24));
            TLRPC.TL_messageMediaPaidMedia paidMedia = (TLRPC.TL_messageMediaPaidMedia) messageObject.messageOwner.media;
            if (!paidMedia.extended_media.isEmpty()) {
                TLRPC.MessageExtendedMedia extMedia = paidMedia.extended_media.get(0);
                ImageLocation location = null;
                if (extMedia instanceof TLRPC.TL_messageExtendedMediaPreview) {
                    TLRPC.TL_messageExtendedMediaPreview m = (TLRPC.TL_messageExtendedMediaPreview) extMedia;
                    location = ImageLocation.getForObject(m.thumb, messageObject.messageOwner);
                } else if (extMedia instanceof TLRPC.TL_messageExtendedMedia) {
                    TLRPC.MessageMedia media = ((TLRPC.TL_messageExtendedMedia) extMedia).media;
                    if (media instanceof TLRPC.TL_messageMediaPhoto) {
                        location = ImageLocation.getForPhoto(FileLoader.getClosestPhotoSizeWithSize(media.photo.sizes, dp(80), true), media.photo);
                    } else if (media instanceof TLRPC.TL_messageMediaDocument) {
                        location = ImageLocation.getForDocument(FileLoader.getClosestPhotoSizeWithSize(media.document.thumbs, dp(80), true), media.document);
                    }
                }
                imageView.setImage(location, "80_80_b2", null, null, null, messageObject);
            }
            topView.addView(imageView, LayoutHelper.createFrame(80, 80, Gravity.CENTER));
        } else if (photo == null) {
            BackupImageView imageView = new BackupImageView(context);
            imageView.setRoundRadius(dp(80));
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setInfo(user);
            imageView.setForUserOrChat(user, avatarDrawable);
            topView.addView(imageView, LayoutHelper.createFrame(80, 80, Gravity.CENTER));
        } else {
            BackupImageView imageView = new BackupImageView(context);
            imageView.setRoundRadius(dp(80));
            imageView.setImage(ImageLocation.getForWebFile(WebFile.createWithWebDocument(photo)), "80_80", null, 0, null);
            topView.addView(imageView, LayoutHelper.createFrame(80, 80, Gravity.CENTER));
        }

        StarsBalanceView balanceView = new StarsBalanceView(context, currentAccount);
        ScaleStateListAnimator.apply(balanceView);
        balanceView.setOnClickListener(v -> {
            if (balanceView.lastBalance <= 0) return;
            BaseFragment lastFragment = LaunchActivity.getLastFragment();
            if (lastFragment != null) {
                BaseFragment.BottomSheetParams bottomSheetParams = new BaseFragment.BottomSheetParams();
                bottomSheetParams.transitionFromLeft = true;
                bottomSheetParams.allowNestedScroll = false;
                lastFragment.showAsSheet(new StarsIntroActivity(), bottomSheetParams);
            }
        });
        topView.addView(balanceView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT, 0, 0, -8, 0));

        linearLayout.addView(topView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 117, Gravity.FILL_HORIZONTAL));

        TextView titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        titleView.setText(getString(R.string.StarsConfirmPurchaseTitle));
        titleView.setGravity(Gravity.CENTER);
        linearLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0));

        TextView subtitleView = new TextView(context);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        if (messageObject != null && messageObject.messageOwner != null && messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPaidMedia) {
            long did = messageObject.getDialogId();
            if (messageObject.messageOwner != null && messageObject.messageOwner.fwd_from != null && messageObject.messageOwner.fwd_from.from_id != null) {
                did = DialogObject.getPeerDialogId(messageObject.messageOwner.fwd_from.from_id);
            }
            if (did < 0 && messageObject.getFromChatId() > 0) {
                final TLRPC.User _user = MessagesController.getInstance(currentAccount).getUser(messageObject.getFromChatId());
                if (_user != null && _user.bot) {
                    did = _user.id;
                }
            }
            final String chatTitle;
            final boolean isBot;
            if (did >= 0) {
                final TLRPC.User _user = MessagesController.getInstance(currentAccount).getUser(did);
                chatTitle = UserObject.getUserName(_user);
                isBot = _user != null && _user.bot;
            } else {
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
                chatTitle = chat == null ? "" : chat.title;
                isBot = false;
            }

            int photosCount = 0, videosCount = 0;
            TLRPC.TL_messageMediaPaidMedia paidMedia = (TLRPC.TL_messageMediaPaidMedia) messageObject.messageOwner.media;
            for (int i = 0; i < paidMedia.extended_media.size(); ++i) {
                TLRPC.MessageExtendedMedia extMedia = paidMedia.extended_media.get(i);
                boolean isVideo = false;
                if (extMedia instanceof TLRPC.TL_messageExtendedMediaPreview) {
                    TLRPC.TL_messageExtendedMediaPreview m = (TLRPC.TL_messageExtendedMediaPreview) extMedia;
                    isVideo = (m.flags & 4) != 0;
                } else if (extMedia instanceof TLRPC.TL_messageExtendedMedia) {
                    TLRPC.MessageMedia media = ((TLRPC.TL_messageExtendedMedia) extMedia).media;
                    isVideo = media instanceof TLRPC.TL_messageMediaDocument;
                }
                if (isVideo) videosCount++;
                else photosCount++;
            }

            String c;
            if (videosCount == 0) {
                c = formatPluralString(isBot ? "StarsConfirmPurchaseMediaBotOne2" : "StarsConfirmPurchaseMediaOne2", (int) stars, photosCount == 1 ? getString(R.string.StarsConfirmPurchaseMedia_SinglePhoto) : formatPluralString("StarsConfirmPurchaseMedia_Photos", photosCount), chatTitle);
            } else if (photosCount == 0) {
                c = formatPluralString(isBot ? "StarsConfirmPurchaseMediaBotOne2" : "StarsConfirmPurchaseMediaOne2", (int) stars, videosCount == 1 ? getString(R.string.StarsConfirmPurchaseMedia_SingleVideo) : formatPluralString("StarsConfirmPurchaseMedia_Videos", videosCount), chatTitle);
            } else {
                c = formatPluralString(isBot ? "StarsConfirmPurchaseMediaBotTwo2" : "StarsConfirmPurchaseMediaTwo2", (int) stars, photosCount == 1 ? getString(R.string.StarsConfirmPurchaseMedia_SinglePhoto) : formatPluralString("StarsConfirmPurchaseMedia_Photos", photosCount), videosCount == 1 ? getString(R.string.StarsConfirmPurchaseMedia_SingleVideo) : formatPluralString("StarsConfirmPurchaseMedia_Videos", videosCount), chatTitle);
            }
            subtitleView.setText(AndroidUtilities.replaceTags(c));
        } else {
            subtitleView.setText(AndroidUtilities.replaceTags(formatPluralStringComma("StarsConfirmPurchaseText", (int) stars, purchase, UserObject.getUserName(user))));
        }
        subtitleView.setMaxWidth(HintView2.cutInFancyHalf(subtitleView.getText(), subtitleView.getPaint()));
        subtitleView.setGravity(Gravity.CENTER);
        linearLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 24));

        ButtonWithCounterView button = new ButtonWithCounterView(context, resourcesProvider);
        button.setText(replaceStars(AndroidUtilities.replaceTags(formatPluralStringComma("StarsConfirmPurchaseButton", (int) stars))), false);
        linearLayout.addView(button, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48));

        b.setCustomView(linearLayout);
        BottomSheet sheet = b.create();

        button.setOnClickListener(v -> {
            if (whenConfirmed != null) {
                sheet.setCanDismissWithSwipe(false);
                button.setLoading(true);
                whenConfirmed.run(close -> {
                    if (close) {
                        sheet.dismiss();
                    } else {
                        AndroidUtilities.runOnUIThread(() -> {
                            sheet.setCanDismissWithSwipe(false);
                            button.setLoading(false);
                        }, 400);
                    }
                });
            } else {
                sheet.dismiss();
            }
        });
        sheet.setOnDismissListener(d -> {
            if (whenDismissed != null) {
                whenDismissed.run();
            }
        });

        sheet.fixNavigationBar();
        sheet.show();
        return sheet;
    }

    public static BottomSheet openStarsChannelInviteSheet(
        Context context,
        Theme.ResourcesProvider resourcesProvider,
        int currentAccount,
        TLRPC.ChatInvite chatInvite,
        Utilities.Callback<Utilities.Callback<Boolean>> whenConfirmed,
        Runnable whenDismissed
    ) {
        BottomSheet.Builder b = new BottomSheet.Builder(context, false, resourcesProvider);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(dp(16), 0, dp(16), dp(8));

        FrameLayout topView = new FrameLayout(context);
        topView.addView(makeParticlesView(context, 40, 0), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        BackupImageView imageView = new BackupImageView(context);
        imageView.setRoundRadius(dp(80));
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        avatarDrawable.setPeerColor(chatInvite.color);
        avatarDrawable.setText(chatInvite.title);
        if (chatInvite.photo != null) {
            TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(chatInvite.photo.sizes, dp(80));
            imageView.setImage(ImageLocation.getForPhoto(photoSize, chatInvite.photo), "80_80", avatarDrawable, chatInvite);
        } else {
            imageView.setImageDrawable(avatarDrawable);
        }

        topView.addView(imageView, LayoutHelper.createFrame(80, 80, Gravity.CENTER));

        Drawable starBg = context.getResources().getDrawable(R.drawable.star_small_outline);
        starBg.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogBackground, resourcesProvider), PorterDuff.Mode.SRC_IN));
        Drawable starFg = context.getResources().getDrawable(R.drawable.star_small_inner);

        ImageView starBgView = new ImageView(context);
        starBgView.setImageDrawable(starBg);
        topView.addView(starBgView, LayoutHelper.createFrame(26, 26, Gravity.CENTER));
        starBgView.setTranslationX(dp(26));
        starBgView.setTranslationY(dp(26));
        starBgView.setScaleX(1.2f);
        starBgView.setScaleY(1.2f);

        ImageView starFgView = new ImageView(context);
        starFgView.setImageDrawable(starFg);
        topView.addView(starFgView, LayoutHelper.createFrame(26, 26, Gravity.CENTER));
        starFgView.setTranslationX(dp(26));
        starFgView.setTranslationY(dp(26));

        StarsBalanceView balanceView = new StarsBalanceView(context, currentAccount);
        ScaleStateListAnimator.apply(balanceView);
        balanceView.setOnClickListener(v -> {
            if (balanceView.lastBalance <= 0) return;
            BaseFragment lastFragment = LaunchActivity.getLastFragment();
            if (lastFragment != null) {
                BaseFragment.BottomSheetParams bottomSheetParams = new BaseFragment.BottomSheetParams();
                bottomSheetParams.transitionFromLeft = true;
                bottomSheetParams.allowNestedScroll = false;
                lastFragment.showAsSheet(new StarsIntroActivity(), bottomSheetParams);
            }
        });
        topView.addView(balanceView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT, 0, 0, -8, 0));

        linearLayout.addView(topView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 117, Gravity.FILL_HORIZONTAL));

        TextView titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        titleView.setText(getString(R.string.StarsSubscribeTitle));
        titleView.setGravity(Gravity.CENTER);
        linearLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0));

        TextView subtitleView = new TextView(context);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        if (chatInvite.subscription_pricing.period == StarsController.PERIOD_MONTHLY) {
            subtitleView.setText(AndroidUtilities.replaceTags(formatPluralString("StarsSubscribeText", (int) chatInvite.subscription_pricing.amount, chatInvite.title)));
        } else {
            final String period = chatInvite.subscription_pricing.period == StarsController.PERIOD_5MINUTES ? "5 minutes" : "a minute";
            subtitleView.setText(AndroidUtilities.replaceTags(formatPluralString("StarsSubscribeTextTest", (int) chatInvite.subscription_pricing.amount, chatInvite.title, period)));
        }
        subtitleView.setMaxWidth(HintView2.cutInFancyHalf(subtitleView.getText(), subtitleView.getPaint()));
        subtitleView.setGravity(Gravity.CENTER);
        linearLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 22));

        if (!TextUtils.isEmpty(chatInvite.about)) {
            TextView aboutView = new TextView(context);
            aboutView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            aboutView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
            aboutView.setText(Emoji.replaceEmoji(chatInvite.about, aboutView.getPaint().getFontMetricsInt(), false));
            aboutView.setGravity(Gravity.CENTER);
            linearLayout.addView(aboutView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 22));
        }

        ButtonWithCounterView button = new ButtonWithCounterView(context, resourcesProvider);
        button.setText(getString(R.string.StarsSubscribeButton), false);
        linearLayout.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        LinkSpanDrawable.LinksTextView infoTextView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
        infoTextView.setText(AndroidUtilities.replaceSingleTag(getString(R.string.StarsSubscribeInfo), () -> {
            Browser.openUrl(context, getString(R.string.StarsSubscribeInfoLink));
        }));
        infoTextView.setGravity(Gravity.CENTER);
        infoTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        infoTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4, resourcesProvider));
        infoTextView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
        linearLayout.addView(infoTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 14, 14, 14, 6));

        b.setCustomView(linearLayout);
        BottomSheet sheet = b.create();

        button.setOnClickListener(v -> {
            if (whenConfirmed != null) {
                sheet.setCanDismissWithSwipe(false);
                button.setLoading(true);
                whenConfirmed.run(close -> {
                    if (close) {
                        sheet.dismiss();
                    } else {
                        AndroidUtilities.runOnUIThread(() -> {
                            sheet.setCanDismissWithSwipe(false);
                            button.setLoading(false);
                        }, 400);
                    }
                });
            } else {
                sheet.dismiss();
            }
        });
        sheet.setOnDismissListener(d -> {
            if (whenDismissed != null) {
                whenDismissed.run();
            }
        });

        sheet.fixNavigationBar(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));
        BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
        if (!AndroidUtilities.isTablet() && lastFragment != null && !AndroidUtilities.hasDialogOnTop(lastFragment)) {
            sheet.makeAttached(lastFragment);
        }

        sheet.show();
        return sheet;
    }

    public static class StarsNeededSheet extends BottomSheetWithRecyclerListView implements NotificationCenter.NotificationCenterDelegate {

        private final long starsNeeded;
        private final HeaderView headerView;
        private final FrameLayout footerView;
        private final FireworksOverlay fireworksOverlay;
        private Runnable whenPurchased;

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.starOptionsLoaded || id == NotificationCenter.starBalanceUpdated) {
                if (adapter != null) {
                    adapter.update(true);
                }
                long balance = StarsController.getInstance(currentAccount).getBalance();
                headerView.titleView.setText(formatPluralStringComma("StarsNeededTitle", (int) (starsNeeded - balance)));
                if (actionBar != null) {
                    actionBar.setTitle(getTitle());
                }
                if (balance >= starsNeeded) {
                    if (whenPurchased != null) {
                        whenPurchased.run();
                        whenPurchased = null;
                        dismiss();
                    }
                }
            }
        }

        @Override
        public void show() {
            long balance = StarsController.getInstance(currentAccount).getBalance();
            if (balance >= starsNeeded) {
                if (whenPurchased != null) {
                    whenPurchased.run();
                    whenPurchased = null;
                }
                return;
            }
            BaseFragment lastFragment = LaunchActivity.getLastFragment();
            if (lastFragment instanceof ChatActivity) {
                ChatActivity chatActivity = (ChatActivity) lastFragment;
                if (chatActivity.isKeyboardVisible() && chatActivity.getChatActivityEnterView() != null) {
                    chatActivity.getChatActivityEnterView().closeKeyboard();
                }
            }
            super.show();
        }

        @Override
        public void dismissInternal() {
            super.dismissInternal();
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starOptionsLoaded);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starBalanceUpdated);
        }

        public static final int TYPE_BOT = 0;
        public static final int TYPE_SUBSCRIPTION_BUY = 1;
        public static final int TYPE_SUBSCRIPTION_KEEP = 2;
        public static final int TYPE_SUBSCRIPTION_REFULFIL = 3;
        public static final int TYPE_LINK = 4;
        public static final int TYPE_REACTIONS = 5;

        public StarsNeededSheet(
            Context context,
            Theme.ResourcesProvider resourcesProvider,
            long starsNeeded,
            int type, String botName,
            Runnable whenPurchased
        ) {
            super(context, null, false, false, false, resourcesProvider);

            topPadding = .2f;

            this.whenPurchased = whenPurchased;
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starOptionsLoaded);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starBalanceUpdated);

            fixNavigationBar();
            recyclerListView.setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, 0);
            recyclerListView.setOnItemClickListener((view, position) -> {
                if (adapter == null) return;
                UItem item = adapter.getItem(position - 1);
                if (item == null) return;
                onItemClick(item, adapter);
            });
            DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
            itemAnimator.setSupportsChangeAnimations(false);
            itemAnimator.setDelayAnimations(false);
            itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            itemAnimator.setDurations(350);
            recyclerListView.setItemAnimator(itemAnimator);
            setBackgroundColor(Theme.getColor(Theme.key_dialogBackgroundGray, resourcesProvider));

            this.starsNeeded = starsNeeded;
            headerView = new HeaderView(context, currentAccount, resourcesProvider);

            long balance = StarsController.getInstance(currentAccount).getBalance();
            headerView.titleView.setText(formatPluralString("StarsNeededTitle", (int) Math.max(0, starsNeeded - balance)));
            String stringRes;
            if (type == TYPE_SUBSCRIPTION_BUY) {
                stringRes = "StarsNeededTextBuySubscription";
            } else if (type == TYPE_SUBSCRIPTION_KEEP) {
                stringRes = "StarsNeededTextKeepSubscription";
            } else if (type == TYPE_SUBSCRIPTION_REFULFIL) {
                stringRes = "StarsNeededTextKeepSubscription";
            } else if (type == TYPE_LINK) {
                stringRes = botName == null ? "StarsNeededTextLink" : "StarsNeededTextLink_" + botName.toLowerCase();
                if (LocaleController.nullable(LocaleController.getString(stringRes)) == null) {
                    stringRes = "StarsNeededTextLink";
                }
            } else if (type == TYPE_REACTIONS) {
                stringRes = "StarsNeededTextReactions";
            } else {
                stringRes = "StarsNeededText";
            }
            if (TextUtils.isEmpty(stringRes)) {
                headerView.subtitleView.setText("");
            } else {
                String str = LocaleController.nullable(formatString(stringRes, LocaleController.getStringResId(stringRes), botName));
                if (str == null) {
                    str = getString(stringRes);
                }
                headerView.subtitleView.setText(AndroidUtilities.replaceTags(str));
                headerView.subtitleView.setMaxWidth(HintView2.cutInFancyHalf(headerView.subtitleView.getText(), headerView.subtitleView.getPaint()));
            }
            actionBar.setTitle(getTitle());

            footerView = new FrameLayout(context);
            LinkSpanDrawable.LinksTextView footerTextView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
            footerView.setPadding(0, dp(11), 0, dp(11));
            footerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            footerTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4, resourcesProvider));
            footerTextView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
            footerTextView.setText(AndroidUtilities.replaceSingleTag(getString(R.string.StarsTOS), () -> {
                Browser.openUrl(getContext(), getString(R.string.StarsTOSLink));
            }));
            footerTextView.setGravity(Gravity.CENTER);
            footerTextView.setMaxWidth(HintView2.cutInFancyHalf(footerTextView.getText(), footerTextView.getPaint()));
            footerView.addView(footerTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
            footerView.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));

            fireworksOverlay = new FireworksOverlay(getContext());
            containerView.addView(fireworksOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            if (adapter != null) {
                adapter.update(false);
            }
        }

        @Override
        protected CharSequence getTitle() {
            if (headerView == null) return null;
            return headerView.titleView.getText();
        }

        private UniversalAdapter adapter;
        @Override
        protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
            return adapter = new UniversalAdapter(recyclerListView, getContext(), currentAccount, 0, true, this::fillItems, resourcesProvider);
        }

        private boolean expanded;
        private final int BUTTON_EXPAND = -1;

        public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asCustom(headerView));
            items.add(UItem.asHeader(getString(R.string.TelegramStarsChoose)));
            int stars = 1;
            ArrayList<TLRPC.TL_starsTopupOption> options = StarsController.getInstance(currentAccount).getOptions();
            if (options != null && !options.isEmpty()) {
                int count = 0;
                int hidden = 0;
                boolean shownNearest = false;
                for (int id = 0; id < options.size(); ++id) {
                    TLRPC.TL_starsTopupOption option = options.get(id);
                    if (option.stars < starsNeeded) {
                        continue;
                    }
                    if (option.extended && !expanded && shownNearest) {
                        hidden++;
                        continue;
                    }
                    items.add(StarTierView.Factory.asStarTier(id, stars++, option));
                    shownNearest = true;
                    count++;
                }
                if (count < 3) {
                    items.clear();
                    items.add(UItem.asCustom(headerView));
                    items.add(UItem.asHeader(getString(R.string.TelegramStarsChoose)));
                    count = 0;
                    for (int id = 0; id < options.size(); ++id) {
                        TLRPC.TL_starsTopupOption option = options.get(id);
                        if (option.stars < starsNeeded) {
                            continue;
                        }
                        items.add(StarTierView.Factory.asStarTier(id, stars++, option));
                        count++;
                    }
                    if (count == 0) {
                        for (int id = 0; id < options.size(); ++id) {
                            TLRPC.TL_starsTopupOption option = options.get(id);
                            items.add(StarTierView.Factory.asStarTier(id, stars++, option));
                            count++;
                        }
                        if (!expanded)
                            items.add(ExpandView.Factory.asExpand(BUTTON_EXPAND, getString(expanded ? R.string.NotifyLessOptions : R.string.NotifyMoreOptions), !expanded).accent());
                    } else {
                        expanded = true;
                    }
                } else if (count > 0) {
                    if (!expanded && hidden > 0)
                        items.add(ExpandView.Factory.asExpand(BUTTON_EXPAND, getString(expanded ? R.string.NotifyLessOptions : R.string.NotifyMoreOptions), !expanded).accent());
                } else {
                    for (int id = 0; id < options.size(); ++id) {
                        items.add(StarTierView.Factory.asStarTier(id, stars++, options.get(id)));
                        count++;
                    }
                }
            } else {
                items.add(UItem.asFlicker(FlickerLoadingView.STAR_TIER));
                items.add(UItem.asFlicker(FlickerLoadingView.STAR_TIER));
                items.add(UItem.asFlicker(FlickerLoadingView.STAR_TIER));
            }
            items.add(UItem.asCustom(footerView));
        }

        public void onItemClick(UItem item, UniversalAdapter adapter) {
            if (item.id == BUTTON_EXPAND) {
                expanded = !expanded;
                adapter.update(true);
            } else if (item.instanceOf(StarTierView.Factory.class)) {
                if (item.object instanceof TLRPC.TL_starsTopupOption) {
                    Activity activity = AndroidUtilities.findActivity(getContext());
                    if (activity == null) {
                        activity = LaunchActivity.instance;
                    }
                    if (activity == null) {
                        return;
                    }
                    StarsController.getInstance(currentAccount).buy(activity, (TLRPC.TL_starsTopupOption) item.object, (success, error) -> {
                        if (getContext() == null) return;
                        if (success) {
                            BulletinFactory.of((FrameLayout) containerView, resourcesProvider).createSimpleBulletin(R.raw.stars_topup, getString(R.string.StarsAcquired), AndroidUtilities.replaceTags(formatPluralString("StarsAcquiredInfo", (int) item.longValue))).show();
                            fireworksOverlay.start(true);
                            StarsController.getInstance(currentAccount).invalidateTransactions(true);
                        } else if (error != null) {
                            BulletinFactory.of((FrameLayout) containerView, resourcesProvider).createSimpleBulletin(R.raw.error, LocaleController.formatString(R.string.UnknownErrorCode, error)).show();
                        }
                    });
                }
            }
        }

        @Override
        public void dismiss() {
            super.dismiss();
            if (headerView != null) {
                headerView.iconView.setPaused(true);
            }
        }

        public static class HeaderView extends LinearLayout {
            private final FrameLayout topView;
            public final StarParticlesView particlesView;
            public final GLIconTextureView iconView;
            public final StarsBalanceView balanceView;
            public final TextView titleView;
            public final TextView subtitleView;

            public HeaderView(Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider) {
                super(context);

                setOrientation(VERTICAL);
                topView = new FrameLayout(context);
                topView.setClipChildren(false);
                topView.setClipToPadding(false);

                particlesView = makeParticlesView(context, 70, 0);
                topView.addView(particlesView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

                iconView = new GLIconTextureView(context, GLIconRenderer.DIALOG_STYLE, Icon3D.TYPE_GOLDEN_STAR);
                iconView.mRenderer.colorKey1 = Theme.key_starsGradient1;
                iconView.mRenderer.colorKey2 = Theme.key_starsGradient2;
                iconView.mRenderer.updateColors();
                iconView.setStarParticlesView(particlesView);
                topView.addView(iconView, LayoutHelper.createFrame(170, 170, Gravity.CENTER, 0, 32, 0, 24));
                iconView.setPaused(false);

                balanceView = new StarsBalanceView(context, currentAccount);
                ScaleStateListAnimator.apply(balanceView);
                balanceView.setOnClickListener(v -> {
                    if (balanceView.lastBalance <= 0) return;
                    BaseFragment lastFragment = LaunchActivity.getLastFragment();
                    if (lastFragment != null) {
                        BaseFragment.BottomSheetParams bottomSheetParams = new BaseFragment.BottomSheetParams();
                        bottomSheetParams.transitionFromLeft = true;
                        bottomSheetParams.allowNestedScroll = false;
                        lastFragment.showAsSheet(new StarsIntroActivity(), bottomSheetParams);
                    }
                });
                topView.addView(balanceView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT, 0, 0, 0, 0));

                addView(topView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 150));

                titleView = new TextView(context);
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                titleView.setTypeface(AndroidUtilities.bold());
                titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
                titleView.setGravity(Gravity.CENTER);
                addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 2, 0, 0));

                subtitleView = new TextView(context);
                subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                subtitleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
                subtitleView.setGravity(Gravity.CENTER);
                addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 9, 0, 18));
            }
        }
    }

    public static class GiftStarsSheet extends BottomSheetWithRecyclerListView implements NotificationCenter.NotificationCenterDelegate {

        private final HeaderView headerView;
        private final FrameLayout footerView;
        private final FireworksOverlay fireworksOverlay;
        private final TLRPC.User user;
        private final Runnable whenPurchased;

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.starGiftOptionsLoaded || id == NotificationCenter.starBalanceUpdated) {
                if (adapter != null) {
                    adapter.update(true);
                }
            }
        }

        @Override
        public void show() {
            BaseFragment lastFragment = LaunchActivity.getLastFragment();
            if (lastFragment instanceof ChatActivity) {
                ChatActivity chatActivity = (ChatActivity) lastFragment;
                if (chatActivity.isKeyboardVisible() && chatActivity.getChatActivityEnterView() != null) {
                    chatActivity.getChatActivityEnterView().closeKeyboard();
                }
            }
            super.show();
        }

        @Override
        public void dismissInternal() {
            super.dismissInternal();
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starGiftOptionsLoaded);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starBalanceUpdated);
        }

        public GiftStarsSheet(
            Context context,
            Theme.ResourcesProvider resourcesProvider,
            TLRPC.User user,
            Runnable whenPurchased
        ) {
            super(context, null, false, false, false, resourcesProvider);

            this.user = user;
            this.whenPurchased = whenPurchased;
            topPadding = .2f;

            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starGiftOptionsLoaded);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starBalanceUpdated);

            fixNavigationBar();
            recyclerListView.setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, 0);
            recyclerListView.setOnItemClickListener((view, position) -> {
                if (adapter == null) return;
                UItem item = adapter.getItem(position - 1);
                if (item == null) return;
                onItemClick(item, adapter);
            });
            DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
            itemAnimator.setSupportsChangeAnimations(false);
            itemAnimator.setDelayAnimations(false);
            itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            itemAnimator.setDurations(350);
            recyclerListView.setItemAnimator(itemAnimator);
            setBackgroundColor(Theme.getColor(Theme.key_dialogBackgroundGray, resourcesProvider));

            headerView = new HeaderView(context, currentAccount, resourcesProvider);

//            long balance = StarsController.getInstance(currentAccount).getBalance();
            headerView.titleView.setText(getString(R.string.GiftStarsTitle));
            headerView.subtitleView.setText(
                TextUtils.concat(
                    AndroidUtilities.replaceTags(formatString(R.string.GiftStarsSubtitle, UserObject.getForcedFirstName(user))),
                    " ",
                    AndroidUtilities.replaceArrows(AndroidUtilities.replaceSingleTag(getString(R.string.GiftStarsSubtitleLinkName).replace(' ', ' '), () -> {
                        StarAppsSheet sheet = new StarAppsSheet(getContext());
                        if (!AndroidUtilities.isTablet() && !AndroidUtilities.hasDialogOnTop(attachedFragment) && attachedFragment != null) {
                            sheet.makeAttached(attachedFragment);
                        }
                        sheet.show();
                    }), true)
                )
            );
            headerView.subtitleView.setMaxWidth(HintView2.cutInFancyHalf(headerView.subtitleView.getText(), headerView.subtitleView.getPaint()) + 1);
            actionBar.setTitle(getTitle());

            AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setInfo(user);
            headerView.avatarImageView.setForUserOrChat(user, avatarDrawable);

            footerView = new FrameLayout(context);
            LinkSpanDrawable.LinksTextView footerTextView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
            footerView.setPadding(0, dp(11), 0, dp(11));
            footerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            footerTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4, resourcesProvider));
            footerTextView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
            footerTextView.setText(AndroidUtilities.replaceSingleTag(getString(R.string.StarsTOS), () -> {
                Browser.openUrl(getContext(), getString(R.string.StarsTOSLink));
            }));
            footerTextView.setGravity(Gravity.CENTER);
            footerTextView.setMaxWidth(HintView2.cutInFancyHalf(footerTextView.getText(), footerTextView.getPaint()));
            footerView.addView(footerTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
            footerView.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));

            fireworksOverlay = new FireworksOverlay(getContext());
            containerView.addView(fireworksOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            if (adapter != null) {
                adapter.update(false);
            }
        }

        @Override
        protected CharSequence getTitle() {
            if (headerView == null) return null;
            return headerView.titleView.getText();
        }

        private UniversalAdapter adapter;
        @Override
        protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
            return adapter = new UniversalAdapter(recyclerListView, getContext(), currentAccount, 0, true, this::fillItems, resourcesProvider);
        }

        private boolean expanded;
        private final int BUTTON_EXPAND = -1;

        public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asCustom(headerView));
            items.add(UItem.asHeader(getString(R.string.TelegramStarsChoose)));
            int stars = 1;
            ArrayList<TLRPC.TL_starsGiftOption> options = StarsController.getInstance(currentAccount).getGiftOptions();
            if (options != null && !options.isEmpty()) {
                int hidden = 0;
                for (int id = 0; id < options.size(); ++id) {
                    TLRPC.TL_starsGiftOption option = options.get(id);
                    if (!expanded && option.extended) {
                        hidden++;
                        continue;
                    }
                    items.add(StarTierView.Factory.asStarTier(id, stars++, option));
                }
                if (!expanded && hidden > 0) {
                    items.add(ExpandView.Factory.asExpand(BUTTON_EXPAND, getString(expanded ? R.string.NotifyLessOptions : R.string.NotifyMoreOptions), !expanded).accent());
                }
            } else {
                items.add(UItem.asFlicker(FlickerLoadingView.STAR_TIER));
                items.add(UItem.asFlicker(FlickerLoadingView.STAR_TIER));
                items.add(UItem.asFlicker(FlickerLoadingView.STAR_TIER));
            }
            items.add(UItem.asCustom(footerView));
        }

        public void onItemClick(UItem item, UniversalAdapter adapter) {
            if (item.id == BUTTON_EXPAND) {
                expanded = !expanded;
                adapter.update(true);
                recyclerListView.smoothScrollBy(0, dp(200), CubicBezierInterpolator.EASE_OUT);
            } else if (item.instanceOf(StarTierView.Factory.class)) {
                if (item.object instanceof TLRPC.TL_starsGiftOption) {
                    Activity activity = AndroidUtilities.findActivity(getContext());
                    if (activity == null) {
                        activity = LaunchActivity.instance;
                    }
                    if (activity == null) {
                        return;
                    }
                    final long userId = user.id;
                    StarsController.getInstance(currentAccount).buyGift(activity, (TLRPC.TL_starsGiftOption) item.object, userId, (success, error) -> {
                        if (getContext() == null) return;
                        if ((success || error != null) && whenPurchased != null) {
                            whenPurchased.run();
                        }
                        dismiss();
                        BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
                        FireworksOverlay fireworksOverlay = LaunchActivity.instance.getFireworksOverlay();
                        if (lastFragment == null) return;
                        if (success) {
                            BulletinFactory.of(lastFragment)
                                .createSimpleBulletin(
                                    R.raw.stars_send,
                                    getString(R.string.StarsGiftSentPopup),
                                    AndroidUtilities.replaceTags(formatPluralString("StarsGiftSentPopupInfo", (int) item.longValue, UserObject.getForcedFirstName(user))),
                                    getString(R.string.ViewInChat),
                                    () -> {
                                        BaseFragment lastFragment2 = LaunchActivity.getSafeLastFragment();
                                        if (lastFragment2 != null) {
                                            lastFragment2.presentFragment(ChatActivity.of(userId));
                                        }
                                    }
                                )
                                .setDuration(Bulletin.DURATION_PROLONG)
                                .show(true);
                            if (fireworksOverlay != null) {
                                fireworksOverlay.start(true);
                            }
                            StarsController.getInstance(currentAccount).invalidateTransactions(true);
                        } else if (error != null) {
                            BulletinFactory.of(lastFragment).createSimpleBulletin(R.raw.error, LocaleController.formatString(R.string.UnknownErrorCode, error)).show();
                        }
                    });
                }
            }
        }

        @Override
        public void dismiss() {
            super.dismiss();
//            if (headerView != null) {
//                headerView.iconView.setPaused(true);
//            }
        }

        public static class HeaderView extends LinearLayout {
            private final FrameLayout topView;
            public final StarParticlesView particlesView;
            public final BackupImageView avatarImageView;
            public final TextView titleView;
            public final LinkSpanDrawable.LinksTextView subtitleView;

            public HeaderView(Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider) {
                super(context);

                setOrientation(VERTICAL);
                topView = new FrameLayout(context);
                topView.setClipChildren(false);
                topView.setClipToPadding(false);

                particlesView = makeParticlesView(context, 70, 0);
                topView.addView(particlesView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

                avatarImageView = new BackupImageView(context);
                avatarImageView.setRoundRadius(dp(50));
                topView.addView(avatarImageView, LayoutHelper.createFrame(100, 100, Gravity.CENTER, 0, 32, 0, 24));

                addView(topView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 150));

                titleView = new TextView(context);
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                titleView.setTypeface(AndroidUtilities.bold());
                titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
                titleView.setGravity(Gravity.CENTER);
                addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 2, 0, 0));

                subtitleView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
                subtitleView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
                subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                subtitleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
                subtitleView.setGravity(Gravity.CENTER);
                addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 9, 0, 18));
            }
        }
    }

    public static SpannableStringBuilder replaceStars(CharSequence cs) {
        return replaceStars(cs, 1.13f);
    }

    public static SpannableStringBuilder replaceStars(CharSequence cs, final float scale) {
        if (cs == null) return null;
        SpannableStringBuilder ssb;
        if (!(cs instanceof SpannableStringBuilder)) {
            ssb = new SpannableStringBuilder(cs);
        } else {
            ssb = (SpannableStringBuilder) cs;
        }
        SpannableString spacedStar = new SpannableString("⭐ ");
        ColoredImageSpan span = new ColoredImageSpan(R.drawable.msg_premium_liststar);
        span.setScale(scale, scale);
        spacedStar.setSpan(span, 0, spacedStar.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        AndroidUtilities.replaceMultipleCharSequence("⭐️", ssb, "⭐");
        AndroidUtilities.replaceMultipleCharSequence("⭐ ", ssb, "⭐");
        AndroidUtilities.replaceMultipleCharSequence("⭐", ssb, spacedStar);
        AndroidUtilities.replaceMultipleCharSequence(StarsController.currency + " ", ssb, StarsController.currency);
        AndroidUtilities.replaceMultipleCharSequence(StarsController.currency, ssb, spacedStar);
        return ssb;
    }

    public static SpannableStringBuilder replaceStars(CharSequence cs, ColoredImageSpan[] spanRef) {
        if (cs == null) return null;
        SpannableStringBuilder ssb;
        if (!(cs instanceof SpannableStringBuilder)) {
            ssb = new SpannableStringBuilder(cs);
        } else {
            ssb = (SpannableStringBuilder) cs;
        }
        ColoredImageSpan span;
        if (spanRef != null && spanRef[0] != null ) {
            span = spanRef[0];
        } else {
            span = new ColoredImageSpan(R.drawable.msg_premium_liststar);
            span.setScale(1.13f, 1.13f);
        }
        if (spanRef != null) {
            spanRef[0] = span;
        }
        SpannableString spacedStar = new SpannableString("⭐ ");
        spacedStar.setSpan(span, 0, spacedStar.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        AndroidUtilities.replaceMultipleCharSequence("⭐️", ssb, "⭐");
        AndroidUtilities.replaceMultipleCharSequence("⭐ ", ssb, "⭐");
        AndroidUtilities.replaceMultipleCharSequence("⭐", ssb, spacedStar);
        AndroidUtilities.replaceMultipleCharSequence(StarsController.currency + " ", ssb, StarsController.currency);
        AndroidUtilities.replaceMultipleCharSequence(StarsController.currency, ssb, spacedStar);
        return ssb;
    }

    public static SpannableStringBuilder replaceStarsWithPlain(CharSequence cs, float scale) {
        if (cs == null) return null;
        SpannableStringBuilder ssb;
        if (!(cs instanceof SpannableStringBuilder)) {
            ssb = new SpannableStringBuilder(cs);
        } else {
            ssb = (SpannableStringBuilder) cs;
        }
        SpannableString spacedStar = new SpannableString("⭐ ");
        ColoredImageSpan span = new ColoredImageSpan(R.drawable.star_small_inner);
        span.recolorDrawable = false;
        span.setScale(scale, scale);
        spacedStar.setSpan(span, 0, spacedStar.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        AndroidUtilities.replaceMultipleCharSequence("⭐️", ssb, "⭐");
        AndroidUtilities.replaceMultipleCharSequence("⭐ ", ssb, "⭐");
        AndroidUtilities.replaceMultipleCharSequence("⭐", ssb, spacedStar);
        AndroidUtilities.replaceMultipleCharSequence(StarsController.currency + " ", ssb, StarsController.currency);
        AndroidUtilities.replaceMultipleCharSequence(StarsController.currency, ssb, spacedStar);
        return ssb;
    }

    public static CharSequence getTransactionTitle(int currentAccount, boolean bot, TLRPC.StarsTransaction t) {
        if (!t.extended_media.isEmpty()) {
            return getString(R.string.StarMediaPurchase);
        }
        if (t.subscription) {
            if (t.subscription_period == StarsController.PERIOD_MONTHLY) {
                return getString(R.string.StarSubscriptionPurchase);
            }
            if (t.subscription_period == StarsController.PERIOD_5MINUTES) {
                return "5-minute subscription fee";
            }
            if (t.subscription_period == StarsController.PERIOD_MINUTE) {
                return "Minute subscription fee";
            }
        }
        if (t.gift) {
            if (t.sent_by != null) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(DialogObject.getPeerDialogId(t.sent_by));
                return getString(UserObject.isUserSelf(user) ? R.string.StarsGiftSent : R.string.StarsGiftReceived);
            }
            return getString(R.string.StarsGiftReceived);
        }
        if (t.title != null) {
            return t.title;
        }
        final long did = DialogObject.getPeerDialogId(t.peer.peer);
        if (did != 0) {
            if (did >= 0) {
                return UserObject.getUserName(MessagesController.getInstance(UserConfig.selectedAccount).getUser(did));
            } else {
                TLRPC.Chat chat = MessagesController.getInstance(UserConfig.selectedAccount).getChat(-did);
                return chat == null ? "" : chat.title;
            }
        } else if (t.peer instanceof TLRPC.TL_starsTransactionPeerFragment) {
            return getString(bot ? R.string.StarsTransactionWithdrawFragment : R.string.StarsTransactionFragment);
        } else if (t.peer instanceof TLRPC.TL_starsTransactionPeerPremiumBot) {
            return getString(R.string.StarsTransactionBot);
        } else {
            return getString(R.string.StarsTransactionUnsupported);
        }
    }

    public static BottomSheet showTransactionSheet(Context context, int currentAccount, int date, TLRPC.Peer sent_by, TLRPC.Peer received_by, TLRPC.TL_messageActionGiftStars action, Theme.ResourcesProvider resourcesProvider) {
        TLRPC.StarsTransaction t = new TLRPC.StarsTransaction();
        t.title = null;
        t.description = null;
        t.photo = null;
        t.peer = new TLRPC.TL_starsTransactionPeer();
        t.peer.peer = sent_by;
        t.date = date;
        t.stars = action.stars;
        t.id = action.transaction_id;
        t.gift = true;
        t.sent_by = sent_by;
        t.received_by = received_by;
        return showTransactionSheet(context, false, 0, currentAccount, t, resourcesProvider);
    }

    public static BottomSheet showTransactionSheet(Context context, int currentAccount, int date, TLRPC.TL_messageActionPaymentRefunded action, Theme.ResourcesProvider resourcesProvider) {
        TLRPC.StarsTransaction t = new TLRPC.StarsTransaction();
        t.title = null;
        t.description = null;
        t.photo = null;
        t.peer = new TLRPC.TL_starsTransactionPeer();
        t.peer.peer = action.peer;
        t.date = date;
        t.stars = action.total_amount;
        t.id = action.charge.id;
        t.refund = true;
        return showTransactionSheet(context, false, 0, currentAccount, t, resourcesProvider);
    }

    public static BottomSheet showTransactionSheet(Context context, boolean bot, int currentAccount, TLRPC.TL_payments_paymentReceiptStars receipt, Theme.ResourcesProvider resourcesProvider) {
        TLRPC.StarsTransaction t = new TLRPC.StarsTransaction();
        t.title = receipt.title;
        t.description = receipt.description;
        t.photo = receipt.photo;
        t.peer = new TLRPC.TL_starsTransactionPeer();
        t.peer.peer = MessagesController.getInstance(currentAccount).getPeer(receipt.bot_id);
        t.date = receipt.date;
        t.stars = -receipt.total_amount;
        t.id = receipt.transaction_id;
        return showTransactionSheet(context, bot, 0, currentAccount, t, resourcesProvider);
    }

    public static BottomSheet showTransactionSheet(Context context, boolean bot, long dialogId, int currentAccount, TLRPC.StarsTransaction transaction, Theme.ResourcesProvider resourcesProvider) {
        if (transaction == null || context == null)
            return null;

        BottomSheet.Builder b = new BottomSheet.Builder(context, false, resourcesProvider);
        BottomSheet[] sheet = new BottomSheet[1];

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(dp(16), dp(transaction.gift ? 0 : 20), dp(16), dp(8));
        linearLayout.setClipChildren(false);
        linearLayout.setClipToPadding(false);

        BackupImageView imageView = new BackupImageView(context);
        if (transaction.gift) {
            final boolean[] played = new boolean[1];
            Runnable setImage = () -> {
                TLRPC.TL_messages_stickerSet set;
                TLRPC.Document document = null;

                String packName = UserConfig.getInstance(currentAccount).premiumGiftsStickerPack;
                if (packName == null) {
                    MediaDataController.getInstance(currentAccount).checkPremiumGiftStickers();
                    return;
                }
                set = MediaDataController.getInstance(currentAccount).getStickerSetByName(packName);
                if (set == null) {
                    set = MediaDataController.getInstance(currentAccount).getStickerSetByEmojiOrName(packName);
                }
                if (set != null) {
                    String emoji;
                    if (transaction.stars <= 1000) {
                        emoji = "2⃣";
                    } else if (transaction.stars < 2500) {
                        emoji = "3⃣";
                    } else {
                        emoji = "4⃣";
                    }
                    for (int i = 0; i < set.packs.size(); ++i) {
                        TLRPC.TL_stickerPack pack = set.packs.get(i);
                        if (TextUtils.equals(pack.emoticon, emoji) && !pack.documents.isEmpty()) {
                            long documentId = pack.documents.get(0);
                            for (int j = 0; j < set.documents.size(); ++j) {
                                TLRPC.Document d = set.documents.get(j);
                                if (d != null && d.id == documentId) {
                                    document = d;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    if (document == null && !set.documents.isEmpty()) {
                        document = set.documents.get(0);
                    }
                }

                final ImageReceiver imageReceiver = imageView.getImageReceiver();
                if (document != null) {
                    imageReceiver.setAllowStartLottieAnimation(true);
                    imageReceiver.setDelegate(new ImageReceiver.ImageReceiverDelegate() {
                        @Override
                        public void didSetImage(ImageReceiver imageReceiver, boolean set, boolean thumb, boolean memCache) {
                            if (set) {
                                RLottieDrawable drawable = imageReceiver.getLottieAnimation();
                                if (drawable != null && !played[0]) {
                                    drawable.setCurrentFrame(0, false);
                                    AndroidUtilities.runOnUIThread(drawable::restart);
                                    played[0] = true;
                                }
                            }
                        }
                    });
                    SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, Theme.key_windowBackgroundGray, 0.3f);
                    imageReceiver.setAutoRepeat(0);
                    imageReceiver.setImage(ImageLocation.getForDocument(document), String.format(Locale.US, "%d_%d_nr", 160, 160), svgThumb, "tgs", set, 1);
                } else {
                    MediaDataController.getInstance(currentAccount).loadStickersByEmojiOrName(packName, false, set == null);
                }
            };
            setImage.run();
            NotificationCenter.getInstance(currentAccount).listen(imageView, NotificationCenter.didUpdatePremiumGiftStickers, args -> setImage.run());
            NotificationCenter.getInstance(currentAccount).listen(imageView, NotificationCenter.diceStickersDidLoad, args -> setImage.run());
            linearLayout.addView(imageView, LayoutHelper.createLinear(160, 160, Gravity.CENTER, 0, -8, 0, 10));
        } else if (!transaction.extended_media.isEmpty()) {
            imageView.setRoundRadius(dp(30));
            TLRPC.MessageMedia media = transaction.extended_media.get(0);
            ImageLocation location = null;
            if (media instanceof TLRPC.TL_messageMediaPhoto) {
                location = ImageLocation.getForPhoto(FileLoader.getClosestPhotoSizeWithSize(media.photo.sizes, dp(100), true), media.photo);
            } else if (media instanceof TLRPC.TL_messageMediaDocument) {
                location = ImageLocation.getForDocument(FileLoader.getClosestPhotoSizeWithSize(media.document.thumbs, dp(100), true), media.document);
            }
            imageView.setImage(location, "100_100", null, null, null, 0);
            linearLayout.addView(imageView, LayoutHelper.createLinear(100, 100, Gravity.CENTER, 0, 0, 0, 10));

            imageView.setOnClickListener(v -> {
                final long did = bot ? dialogId : DialogObject.getPeerDialogId(transaction.peer.peer);
                ArrayList<MessageObject> messages = new ArrayList<>();
                for (int i = 0; i < transaction.extended_media.size(); ++i) {
                    TLRPC.MessageMedia emedia = transaction.extended_media.get(i);
                    TLRPC.TL_message msg = new TLRPC.TL_message();
                    msg.id = transaction.msg_id;
                    msg.dialog_id = did;
                    msg.from_id = new TLRPC.TL_peerChannel();
                    msg.from_id.channel_id = -did;
                    msg.peer_id = new TLRPC.TL_peerChannel();
                    msg.peer_id.channel_id = -did;
                    msg.date = transaction.date;
                    msg.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA;
                    msg.media = emedia;
                    msg.noforwards = true;
                    MessageObject msgObj = new MessageObject(currentAccount, msg, false, false);
                    messages.add(msgObj);
                }
                if (messages.isEmpty()) return;

                PhotoViewer.getInstance().setParentActivity(LaunchActivity.getLastFragment(), resourcesProvider);
                PhotoViewer.getInstance().openPhoto(messages, 0, did, 0, 0, new PhotoViewer.EmptyPhotoViewerProvider() {

                    @Override
                    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview) {
                        final ImageReceiver imageReceiver = imageView.getImageReceiver();
                        int[] coords = new int[2];
                        imageView.getLocationInWindow(coords);
                        PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                        object.viewX = coords[0];
                        object.viewY = coords[1] - (Build.VERSION.SDK_INT >= 21 ? 0 : AndroidUtilities.statusBarHeight);
                        object.parentView = linearLayout;
                        object.animatingImageView = null;
                        object.imageReceiver = imageReceiver;
                        if (needPreview) {
                            object.thumb = imageReceiver.getBitmapSafe();
                        }
                        object.radius = imageReceiver.getRoundRadius(true);
                        object.dialogId = did;
                        object.clipTopAddition = 0; // (int) (chatListViewPaddingTop - chatListViewPaddingVisibleOffset - AndroidUtilities.dp(4));
                        object.clipBottomAddition = 0; // blurredViewBottomOffset;
                        return object;
                    }

                    @Override
                    public boolean forceAllInGroup() {
                        return true;
                    }

                    @Override
                    public boolean validateGroupId(long groupId) {
                        return false;
                    }
                });
            });
        } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeer) {
            if (transaction.photo != null) {
                imageView.setRoundRadius(dp(50));
                imageView.setImage(ImageLocation.getForWebFile(WebFile.createWithWebDocument(transaction.photo)), "100_100", null, 0, null);
            } else {
                imageView.setRoundRadius(dp(50));
                final long did = transaction.subscription && bot ? dialogId : DialogObject.getPeerDialogId(transaction.peer.peer);
                AvatarDrawable avatarDrawable = new AvatarDrawable();
                if (did >= 0) {
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(did);
                    avatarDrawable.setInfo(user);
                    imageView.setForUserOrChat(user, avatarDrawable);
                } else {
                    TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
                    avatarDrawable.setInfo(chat);
                    imageView.setForUserOrChat(chat, avatarDrawable);
                }
            }
            linearLayout.addView(imageView, LayoutHelper.createLinear(100, 100, Gravity.CENTER, 0, 0, 0, 10));
        } else {
            String platform = "?";
            if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerAppStore) {
                platform = "ios";
            } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerPlayMarket) {
                platform = "android";
            } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerPremiumBot) {
                platform = "premiumbot";
            } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerFragment) {
                platform = "fragment";
            } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerAds) {
                platform = "ads";
            }
            CombinedDrawable drawable = (CombinedDrawable) SessionCell.createDrawable(100, platform);
            drawable.setIconSize(dp(40), dp(40));
            imageView.setImageDrawable(drawable);
            // linearLayout.addView(imageView, LayoutHelper.createLinear(100, 100, Gravity.CENTER, 0, 0, 0, 10));
        }

        TextView textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setGravity(Gravity.CENTER);
        textView.setText(getTransactionTitle(currentAccount, bot, transaction));
        linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 0, 20, 4));

        textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(Theme.getColor(transaction.stars >= 0 ? Theme.key_color_green : Theme.key_color_red, resourcesProvider));
        textView.setText(replaceStarsWithPlain((transaction.stars >= 0 ? "+" : "-") + LocaleController.formatNumber((int) Math.abs(transaction.stars), ' ') + " ⭐️", .8f));
        SpannableStringBuilder s = new SpannableStringBuilder(textView.getText());
        if (transaction.refund) {
            appendStatus(s, textView, getString(R.string.StarsRefunded));
        } else if (transaction.failed) {
            textView.setTextColor(Theme.getColor(Theme.key_color_red, resourcesProvider));
            appendStatus(s, textView, getString(R.string.StarsFailed));
        } else if (transaction.pending) {
            textView.setTextColor(Theme.getColor(Theme.key_color_yellow, resourcesProvider));
            appendStatus(s, textView, getString(R.string.StarsPending));
        }
        textView.setText(s);
        linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 0, 20, 4));

        if (transaction.gift) {
            final TLRPC.User user =   transaction.sent_by == null ? null : MessagesController.getInstance(currentAccount).getUser(DialogObject.getPeerDialogId(transaction.sent_by));
            final TLRPC.User received = transaction.sent_by == null ? null : MessagesController.getInstance(currentAccount).getUser(DialogObject.getPeerDialogId(transaction.received_by));
            final boolean self = UserObject.isUserSelf(user);

            if (self) {
                textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
                textView.setText(replaceStarsWithPlain(LocaleController.formatNumber((int) Math.abs(transaction.stars), ' ') + " ⭐️", .8f));
            }

            textView = new LinkSpanDrawable.LinksTextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setGravity(Gravity.CENTER);
            textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
            ((LinkSpanDrawable.LinksTextView) textView).setDisablePaddingsOffsetY(true);
            textView.setText(TextUtils.concat(
                AndroidUtilities.replaceTags(self ? formatString(R.string.ActionGiftStarsSubtitle, UserObject.getForcedFirstName(received)) : getString(R.string.ActionGiftStarsSubtitleYou)),
                " ",
                AndroidUtilities.replaceArrows(AndroidUtilities.replaceSingleTag(getString(R.string.GiftStarsSubtitleLinkName).replace(' ', ' '), () -> {
                    StarAppsSheet sheet1 = new StarAppsSheet(context);
                    if (!AndroidUtilities.isTablet() && !AndroidUtilities.hasDialogOnTop(sheet[0].attachedFragment) && sheet[0] != null && sheet[0].attachedFragment != null) {
                        sheet1.makeAttached(sheet[0].attachedFragment);
                    }
                    sheet1.show();
                }), true)
            ));
            linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 0, 20, 4));
        } else if (transaction.description != null && transaction.extended_media.isEmpty()) {
            textView = new TextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setGravity(Gravity.CENTER);
            textView.setText(transaction.description);
            linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 0, 20, 4));
        }

        TableView tableView = new TableView(context, resourcesProvider);
        if (transaction.peer instanceof TLRPC.TL_starsTransactionPeer) {
            final long did = DialogObject.getPeerDialogId(transaction.peer.peer);
            textView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
            textView.setPadding(dp(12.66f), dp(9.33f), dp(12.66f), dp(9.33f));
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
            textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            textView.setSingleLine(true);
            ((LinkSpanDrawable.LinksTextView) textView).setDisablePaddingsOffsetY(true);
            AvatarSpan avatarSpan = new AvatarSpan(textView, currentAccount, 24);
            CharSequence username;
            boolean deleted = false;
            final boolean unknown;
            if (UserObject.isService(did)) {
                deleted = false;
                unknown = true;
                username = getString(R.string.StarsTransactionUnknown);
                CombinedDrawable iconDrawable = getPlatformDrawable("fragment");
                iconDrawable.setIconSize(dp(16), dp(16));
                avatarSpan.setImageDrawable(iconDrawable);
            } else if (did >= 0) {
                unknown = false;
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(did);
                deleted = user == null;
                username = UserObject.getUserName(user);
                avatarSpan.setUser(user);
            } else {
                unknown = false;
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
                deleted = chat == null;
                username = chat == null ? "" : chat.title;
                avatarSpan.setChat(chat);
            }
            SpannableStringBuilder ssb = new SpannableStringBuilder("x  " + username);
            ssb.setSpan(avatarSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    sheet[0].dismiss();
                    if (unknown) {
                        Browser.openUrl(context, getString(R.string.StarsTransactionUnknownLink));
                    } else {
                        BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
                        if (lastFragment != null) {
                            lastFragment.presentFragment(ChatActivity.of(did));
                        }
                    }
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setUnderlineText(false);
                }
            }, 3, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(ssb);
            if (!deleted) {
                tableView.addRowUnpadded(getString(transaction.subscription && !bot ? R.string.StarSubscriptionTo : R.string.StarsTransactionRecipient), textView);
            }
        } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerFragment) {
            if (transaction.gift) {
                textView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
                textView.setPadding(dp(12.66f), dp(9.33f), dp(12.66f), dp(9.33f));
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
                textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                textView.setSingleLine(true);
                ((LinkSpanDrawable.LinksTextView) textView).setDisablePaddingsOffsetY(true);
                AvatarSpan avatarSpan = new AvatarSpan(textView, currentAccount, 24);
                CharSequence username = getString(R.string.StarsTransactionUnknown);
                CombinedDrawable iconDrawable = getPlatformDrawable("fragment", 24);
                iconDrawable.setIconSize(dp(16), dp(16));
                avatarSpan.setImageDrawable(iconDrawable);
                SpannableStringBuilder ssb = new SpannableStringBuilder("x  " + username);
                ssb.setSpan(avatarSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        sheet[0].dismiss();
                        Browser.openUrl(context, getString(R.string.StarsTransactionUnknownLink));
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setUnderlineText(false);
                    }
                }, 3, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.setText(ssb);
                tableView.addRowUnpadded(getString(R.string.StarsTransactionRecipient), textView);
            } else {
                tableView.addRow(getString(R.string.StarsTransactionSource), getString(R.string.Fragment));
            }
        } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerAppStore) {
            tableView.addRow(getString(R.string.StarsTransactionSource), getString(R.string.AppStore));
        } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerPlayMarket) {
            tableView.addRow(getString(R.string.StarsTransactionSource), getString(R.string.PlayMarket));
        } else if (transaction.peer instanceof TLRPC.TL_starsTransactionPeerPremiumBot) {
            tableView.addRow(getString(R.string.StarsTransactionSource), getString(R.string.StarsTransactionBot));
        }

        if (transaction.peer instanceof TLRPC.TL_starsTransactionPeer && (transaction.flags & 256) != 0) {
            long did = DialogObject.getPeerDialogId(transaction.peer.peer);
            if (bot) {
                did = dialogId;
            }
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
            if (chat != null) {
                textView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
                textView.setPadding(dp(12.66f), dp(9.33f), dp(12.66f), dp(9.33f));
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
                textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                ((LinkSpanDrawable.LinksTextView) textView).setDisablePaddingsOffsetY(true);
                SpannableStringBuilder ssb = new SpannableStringBuilder("");
                if (!transaction.extended_media.isEmpty()) {
                    int count = 0;
                    for (TLRPC.MessageMedia media : transaction.extended_media) {
                        ImageReceiverSpan span = new ImageReceiverSpan(textView, currentAccount, 24);
                        ImageLocation location = null;
                        if (media instanceof TLRPC.TL_messageMediaPhoto) {
                            location = ImageLocation.getForPhoto(FileLoader.getClosestPhotoSizeWithSize(media.photo.sizes, dp(24), true), media.photo);
                        } else if (media instanceof TLRPC.TL_messageMediaDocument) {
                            location = ImageLocation.getForDocument(FileLoader.getClosestPhotoSizeWithSize(media.document.thumbs, dp(24), true), media.document);
                        }
                        if (location != null) {
                            span.setRoundRadius(6);
                            span.imageReceiver.setImage(location, "24_24", null, null, null, 0);
                            SpannableString str = new SpannableString("x");
                            str.setSpan(span, 0, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ssb.append(str);
                            ssb.append(" ");
                            count++;
                        }
                        if (count >= 3) break;
                    }
                }
                ssb.append(" ");
                int start = ssb.length();
                String username = ChatObject.getPublicUsername(chat);
                if (TextUtils.isEmpty(username)) {
                    ssb.append(chat.title);
                } else {
                    ssb.append(MessagesController.getInstance(currentAccount).linkPrefix + "/" + username + "/" + transaction.msg_id);
                }
                final long finalDialogId = did;
                Runnable open = () -> {
                    sheet[0].dismiss();
                    BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
                    if (lastFragment != null) {
                        Bundle args = new Bundle();
                        args.putLong("chat_id", -finalDialogId);
                        args.putInt("message_id", transaction.msg_id);
                        lastFragment.presentFragment(new ChatActivity(args));
                    }
                };
                ssb.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        open.run();
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setUnderlineText(false);
                    }
                }, start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setText(ssb);
                textView.setOnClickListener(v -> open.run());
                tableView.addRowUnpadded(getString(transaction.reaction ? R.string.StarsTransactionMessage : R.string.StarsTransactionMedia), textView);
            }
        }

        if (!TextUtils.isEmpty(transaction.id)) {
            FrameLayout idLayout = new FrameLayout(context);
            idLayout.setPadding(dp(12.66f), dp(9.33f), dp(10.66f), dp(9.33f));
            textView = new TextView(context);
            textView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MONO));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
            textView.setMaxLines(4);
            textView.setSingleLine(false);
            textView.setText(transaction.id);
            idLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 0, 0, 32, 0));
            ImageView copyView = new ImageView(context);
            copyView.setImageResource(R.drawable.msg_copy);
            copyView.setScaleType(ImageView.ScaleType.CENTER);
            copyView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon, resourcesProvider), PorterDuff.Mode.SRC_IN));
            copyView.setOnClickListener(v -> {
                AndroidUtilities.addToClipboard(transaction.id);
                BulletinFactory.of(sheet[0].topBulletinContainer, resourcesProvider).createSimpleBulletin(R.raw.copy, getString(R.string.StarsTransactionIDCopied)).show(false);
            });
            ScaleStateListAnimator.apply(copyView);
            copyView.setBackground(Theme.createSelectorDrawable(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon, resourcesProvider), .10f), Theme.RIPPLE_MASK_ROUNDRECT_6DP));
            idLayout.addView(copyView, LayoutHelper.createFrame(30, 30, Gravity.RIGHT | Gravity.CENTER_VERTICAL));

            tableView.addRowUnpadded(getString(R.string.StarsTransactionID), idLayout);
        }

        tableView.addRow(getString(R.string.StarsTransactionDate), LocaleController.formatString(R.string.formatDateAtTime, LocaleController.getInstance().getFormatterGiveawayCard().format(new Date(transaction.date * 1000L)), LocaleController.getInstance().getFormatterDay().format(new Date(transaction.date * 1000L))));
        linearLayout.addView(tableView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 17, 0, 0));

        if ((transaction.flags & 32) != 0) {
            tableView.addRow(getString(R.string.StarsTransactionTONDate), LocaleController.formatString(R.string.formatDateAtTime, LocaleController.getInstance().getFormatterGiveawayCard().format(new Date(transaction.transaction_date * 1000L)), LocaleController.getInstance().getFormatterDay().format(new Date(transaction.transaction_date * 1000L))));
        }

        textView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setText(AndroidUtilities.replaceSingleTag(getString(R.string.StarsTransactionTOS), () -> {
            Browser.openUrl(context, getString(R.string.StarsTOSLink));
        }));
        textView.setGravity(Gravity.CENTER);
        linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 15, 0, 15));

        ButtonWithCounterView button = new ButtonWithCounterView(context, resourcesProvider);
        if ((transaction.flags & 32) != 0) {
            button.setText(getString(R.string.StarsTransactionViewInBlockchainExplorer), false);
        } else {
            button.setText(getString(R.string.OK), false);
        }
        linearLayout.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        b.setCustomView(linearLayout);
        sheet[0] = b.create();
        sheet[0].useBackgroundTopPadding = false;
        if ((transaction.flags & 32) != 0) {
            button.setOnClickListener(v -> {
                Browser.openUrl(context, transaction.transaction_url);
            });
        } else {
            button.setOnClickListener(v -> {
                sheet[0].dismiss();
            });
        }

        sheet[0].fixNavigationBar();
        BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (!AndroidUtilities.isTablet() && !AndroidUtilities.hasDialogOnTop(fragment)) {
            sheet[0].makeAttached(fragment);
        }
        sheet[0].show();
        return sheet[0];
    }

    public static BottomSheet showSubscriptionSheet(Context context, int currentAccount, TLRPC.StarsSubscription subscription, Theme.ResourcesProvider resourcesProvider) {
        if (subscription == null || context == null)
            return null;

        BottomSheet.Builder b = new BottomSheet.Builder(context, false, resourcesProvider);
        BottomSheet[] sheet = new BottomSheet[1];

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(dp(16), dp(20), dp(16), dp(4));
        linearLayout.setClipChildren(false);
        linearLayout.setClipToPadding(false);

        FrameLayout topView = new FrameLayout(context);
        linearLayout.addView(topView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL_HORIZONTAL, 0, 0, 0, 10));

        BackupImageView imageView = new BackupImageView(context);
        imageView.setRoundRadius(dp(50));
        final long did = DialogObject.getPeerDialogId(subscription.peer);
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        if (did >= 0) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(did);
            avatarDrawable.setInfo(user);
            imageView.setForUserOrChat(user, avatarDrawable);
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
            avatarDrawable.setInfo(chat);
            imageView.setForUserOrChat(chat, avatarDrawable);
        }
        topView.addView(imageView, LayoutHelper.createFrame(100, 100, Gravity.CENTER));

        Drawable starBg = context.getResources().getDrawable(R.drawable.star_small_outline);
        starBg.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogBackground, resourcesProvider), PorterDuff.Mode.SRC_IN));
        Drawable starFg = context.getResources().getDrawable(R.drawable.star_small_inner);

        ImageView starBgView = new ImageView(context);
        starBgView.setImageDrawable(starBg);
        topView.addView(starBgView, LayoutHelper.createFrame(28, 28, Gravity.CENTER));
        starBgView.setTranslationX(dp(34));
        starBgView.setTranslationY(dp(35));
        starBgView.setScaleX(1.1f);
        starBgView.setScaleY(1.1f);

        ImageView starFgView = new ImageView(context);
        starFgView.setImageDrawable(starFg);
        topView.addView(starFgView, LayoutHelper.createFrame(28, 28, Gravity.CENTER));
        starFgView.setTranslationX(dp(34));
        starFgView.setTranslationY(dp(35));

        TextView textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setGravity(Gravity.CENTER);
        textView.setText(getString(R.string.StarsSubscriptionTitle));
        linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 0, 20, 4));

        textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4, resourcesProvider));
        if (subscription.pricing.period == StarsController.PERIOD_MONTHLY) {
            textView.setText(replaceStarsWithPlain(formatString(R.string.StarsSubscriptionPrice, subscription.pricing.amount), .8f));
        } else {
            final String period = subscription.pricing.period == StarsController.PERIOD_5MINUTES ? "5min" : "min";
            textView.setText(replaceStarsWithPlain(formatString(R.string.StarsSubscriptionPrice, subscription.pricing.amount, period), .8f));
        }
        linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 0, 20, 4));

        TableView tableView = new TableView(context, resourcesProvider);
        textView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
        textView.setPadding(dp(12.66f), dp(9.33f), dp(12.66f), dp(9.33f));
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
        textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setSingleLine(true);
        ((LinkSpanDrawable.LinksTextView) textView).setDisablePaddingsOffsetY(true);
        AvatarSpan avatarSpan = new AvatarSpan(textView, currentAccount, 24);
        CharSequence username;
        boolean deleted = false;
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
        deleted = chat == null;
        username = chat == null ? "" : chat.title;
        avatarSpan.setChat(chat);
        SpannableStringBuilder ssb = new SpannableStringBuilder("x  " + username);
        ssb.setSpan(avatarSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                sheet[0].dismiss();
                BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
                if (lastFragment != null) {
                    lastFragment.presentFragment(ChatActivity.of(did));
                }
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setUnderlineText(false);
            }
        }, 3, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ssb);
        if (!deleted) {
            tableView.addRowUnpadded(getString(R.string.StarsSubscriptionChannel), textView);
        }

        tableView.addRow(
                getString(R.string.StarsSubscriptionSince),
                LocaleController.formatString(R.string.formatDateAtTime, LocaleController.getInstance().getFormatterGiveawayCard().format(new Date((subscription.until_date - subscription.pricing.period) * 1000L)), LocaleController.getInstance().getFormatterDay().format(new Date((subscription.until_date - subscription.pricing.period) * 1000L)))
        );
        final long now = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        tableView.addRow(
                getString(subscription.canceled ? R.string.StarsSubscriptionUntilExpires : now > subscription.until_date ? R.string.StarsSubscriptionUntilExpired : R.string.StarsSubscriptionUntilRenews),
                LocaleController.formatString(R.string.formatDateAtTime, LocaleController.getInstance().getFormatterGiveawayCard().format(new Date(subscription.until_date * 1000L)), LocaleController.getInstance().getFormatterDay().format(new Date(subscription.until_date * 1000L)))
        );
        linearLayout.addView(tableView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 17, 0, 0));

        textView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setText(AndroidUtilities.replaceSingleTag(getString(R.string.StarsTransactionTOS), () -> {
            Browser.openUrl(context, getString(R.string.StarsTOSLink));
        }));
        textView.setGravity(Gravity.CENTER);
        linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 14, 15, 14, 7));

        if (now < subscription.until_date) {
            if (subscription.can_refulfill) {
                textView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
                textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
                textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                textView.setText(formatString(R.string.StarsSubscriptionRefulfillInfo, LocaleController.formatDateChat(subscription.until_date)));
                textView.setSingleLine(false);
                textView.setMaxLines(4);
                textView.setGravity(Gravity.CENTER);
                linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 26, 7, 26, 15));

                ButtonWithCounterView button = new ButtonWithCounterView(context, true, resourcesProvider);
                button.setText(getString(R.string.StarsSubscriptionRefulfill), false);
                linearLayout.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
                button.setOnClickListener(v -> {
                    if (button.isLoading()) return;
                    StarsController c = StarsController.getInstance(currentAccount);
                    final Runnable refulfil = () -> {
                        button.setLoading(true);
                        TLRPC.TL_fulfillStarsSubscription req = new TLRPC.TL_fulfillStarsSubscription();
                        req.subscription_id = subscription.id;
                        req.peer = new TLRPC.TL_inputPeerSelf();
                        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                            button.setLoading(false);
                            if (sheet[0] != null) {
                                sheet[0].dismiss();
                            }
                            StarsController.getInstance(currentAccount).invalidateSubscriptions(true);

                            BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
                            if (lastFragment == null) return;
                            lastFragment.presentFragment(ChatActivity.of(did));
                        }));
                    };
                    if (c.balance < subscription.pricing.amount) {
                        new StarsNeededSheet(context, resourcesProvider, subscription.pricing.amount, StarsNeededSheet.TYPE_SUBSCRIPTION_KEEP, chat == null ? "" : chat.title, refulfil).show();
                    } else {
                        refulfil.run();
                    }
                });
            } else if (subscription.canceled) {
                textView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
                textView.setTextColor(Theme.getColor(Theme.key_color_red, resourcesProvider));
                textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                textView.setText(getString(R.string.StarsSubscriptionCancelledText));
                textView.setSingleLine(false);
                textView.setMaxLines(4);
                textView.setGravity(Gravity.CENTER);
                linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 26, 7, 26, 15));

                if (subscription.chat_invite_hash != null) {
                    ButtonWithCounterView button = new ButtonWithCounterView(context, true, resourcesProvider);
                    button.setText(getString(R.string.StarsSubscriptionRenew), false);
                    linearLayout.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
                    button.setOnClickListener(v -> {
                        if (button.isLoading()) return;
                        button.setLoading(true);
                        TLRPC.TL_changeStarsSubscription req = new TLRPC.TL_changeStarsSubscription();
                        req.canceled = false;
                        req.peer = new TLRPC.TL_inputPeerSelf();
                        req.subscription_id = subscription.id;
                        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                            button.setLoading(false);
                            if (sheet[0] != null) {
                                sheet[0].dismiss();
                            }
                            StarsController.getInstance(currentAccount).invalidateSubscriptions(true);

                            BaseFragment fragment = LaunchActivity.getSafeLastFragment();
                            if (fragment != null) {
                                BulletinFactory.of(fragment).createUsersBulletin(Collections.singletonList(chat), getString(R.string.StarsSubscriptionRenewedToast), AndroidUtilities.replaceTags(formatString(R.string.StarsSubscriptionRenewedToastText, chat == null ? "" : chat.title))).show(false);
                            }
                        }));
                    });
                }
            } else {
                textView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
                textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
                textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                textView.setText(formatString(R.string.StarsSubscriptionCancelInfo, LocaleController.formatDateChat(subscription.until_date)));
                textView.setSingleLine(false);
                textView.setMaxLines(4);
                textView.setGravity(Gravity.CENTER);
                linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 26, 7, 26, 15));

                ButtonWithCounterView button = new ButtonWithCounterView(context, false, resourcesProvider);
                button.setText(getString(R.string.StarsSubscriptionCancel), false);
                button.setTextColor(Theme.getColor(Theme.key_color_red, resourcesProvider));
                linearLayout.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
                button.setOnClickListener(v -> {
                    if (button.isLoading()) return;
                    button.setLoading(true);
                    TLRPC.TL_changeStarsSubscription req = new TLRPC.TL_changeStarsSubscription();
                    req.canceled = true;
                    req.peer = new TLRPC.TL_inputPeerSelf();
                    req.subscription_id = subscription.id;
                    ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        button.setLoading(false);
                        if (sheet[0] != null) {
                            sheet[0].dismiss();
                        }
                        StarsController.getInstance(currentAccount).invalidateSubscriptions(true);

                        BaseFragment fragment = LaunchActivity.getSafeLastFragment();
                        if (fragment != null) {
                            BulletinFactory.of(fragment).createUsersBulletin(Collections.singletonList(chat), getString(R.string.StarsSubscriptionCancelledToast), AndroidUtilities.replaceTags(formatString(R.string.StarsSubscriptionCancelledToastText, LocaleController.formatDateChat(subscription.until_date)))).show(false);
                        }
                    }));
                });
            }
        } else {
            textView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
            textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            textView.setText(formatString(R.string.StarsSubscriptionExpiredInfo, LocaleController.formatDateChat(subscription.until_date)));
            textView.setSingleLine(false);
            textView.setMaxLines(4);
            textView.setGravity(Gravity.CENTER);
            linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 26, 7, 26, 15));

            if (subscription.chat_invite_hash != null) {
                ButtonWithCounterView button = new ButtonWithCounterView(context, true, resourcesProvider);
                button.setText(getString(R.string.StarsSubscriptionAgain), false);
                linearLayout.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
                button.setOnClickListener(v -> {
                    if (button.isLoading()) return;
                    button.setLoading(true);
                    TLRPC.TL_messages_checkChatInvite req = new TLRPC.TL_messages_checkChatInvite();
                    req.hash = subscription.chat_invite_hash;
                    ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        button.setLoading(false);
                        if (res instanceof TLRPC.ChatInvite) {
                            TLRPC.ChatInvite invite = (TLRPC.ChatInvite) res;
                            if (invite.subscription_pricing == null) { // wtf
                                BulletinFactory.of(sheet[0].topBulletinContainer, resourcesProvider).createErrorBulletin(getString(R.string.UnknownError)).show(false);
                                return;
                            }
                            final long stars = invite.subscription_pricing.amount;
                            StarsController.getInstance(currentAccount).subscribeTo(req.hash, invite, (status, dialogId) -> {
                                if ("paid".equals(status) && dialogId != 0) {
                                    AndroidUtilities.runOnUIThread(() -> {
                                        BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
                                        if (lastFragment == null) return;
                                        BaseFragment chatActivity = ChatActivity.of(dialogId);
                                        lastFragment.presentFragment(chatActivity);

                                        TLRPC.Chat newChat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
                                        if (newChat != null) {
                                            AndroidUtilities.runOnUIThread(() -> {
                                                BulletinFactory.of(chatActivity).createSimpleBulletin(R.raw.stars_send, getString(R.string.StarsSubscriptionCompleted), AndroidUtilities.replaceTags(formatPluralString("StarsSubscriptionCompletedText", (int) stars, newChat.title))).show(true);
                                            }, 250);
                                        }
                                    });
                                }
                            });
                        } else {
                            BulletinFactory.of(sheet[0].topBulletinContainer, resourcesProvider).createErrorBulletin(LocaleController.getString(R.string.LinkHashExpired)).show(false);
                        }
                    }));
                });
            }
        }

        b.setCustomView(linearLayout);
        sheet[0] = b.create();
        sheet[0].useBackgroundTopPadding = false;

        sheet[0].fixNavigationBar();
        BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (!AndroidUtilities.isTablet() && !AndroidUtilities.hasDialogOnTop(fragment)) {
            sheet[0].makeAttached(fragment);
        }
        sheet[0].show();
        return sheet[0];
    }

    private static CharSequence appendStatus(SpannableStringBuilder s, TextView textView, String string) {
        s.append(" ");
        SpannableString refund = new SpannableString(string);
        final int color = textView.getCurrentTextColor();
        refund.setSpan(new ReplacementSpan() {
            private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            { backgroundPaint.setColor(Theme.multAlpha(color, .10f)); }
            private final Text layout = new Text(string, 13, AndroidUtilities.bold());

            @Override
            public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
                return (int) (dp(6 + 6) + layout.getCurrentWidth());
            }

            @Override
            public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
                AndroidUtilities.rectTmp.set(x, (top + bottom - dp(20)) / 2f, x + dp(6 + 6) + layout.getCurrentWidth(), (top + bottom + dp(20)) / 2f);
                canvas.drawRoundRect(AndroidUtilities.rectTmp, dp(4), dp(4), backgroundPaint);
                layout.draw(canvas, x + dp(6), (top + bottom) / 2f, color, 1f);
            }
        }, 0, refund.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.append(refund);
        return s;
    }

    public static BottomSheet showMediaPriceSheet(Context context, long stars, boolean allowClear, Utilities.Callback2<Long, Runnable> whenDone, Theme.ResourcesProvider resourcesProvider) {
        final BottomSheet.Builder b = new BottomSheet.Builder(context, false, resourcesProvider);
        final BottomSheet[] sheet = new BottomSheet[1];

        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setClipChildren(false);
        linearLayout.setClipToPadding(false);
        linearLayout.setPadding(dp(16), dp(16), dp(16), dp(8));

        final TextView titleView = new TextView(context);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setText(getString(R.string.PaidContentTitle));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        linearLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 0, 4, 18));

        final EditTextBoldCursor editText = new EditTextBoldCursor(context);
        final OutlineTextContainerView editTextContainer = new OutlineTextContainerView(context, resourcesProvider);
        editTextContainer.setForceForceUseCenter(true);
        editTextContainer.setText(getString(R.string.PaidContentPriceTitle));
        editTextContainer.setLeftPadding(dp(14 + 22));
        editText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        editText.setBackground(null);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        editText.setMaxLines(1);
        int padding = AndroidUtilities.dp(16);
        editText.setPadding(dp(6), padding, padding, padding);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setTypeface(Typeface.DEFAULT);
        editText.setSelectAllOnFocus(true);
        editText.setHighlightColor(Theme.getColor(Theme.key_chat_inTextSelectionHighlight, resourcesProvider));
        editText.setHandlesColor(Theme.getColor(Theme.key_chat_TextSelectionCursor, resourcesProvider));
        editText.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        editText.setOnFocusChangeListener((v, hasFocus) -> editTextContainer.animateSelection(hasFocus, !TextUtils.isEmpty(editText.getText())));
        LinearLayout editTextLayout = new LinearLayout(context);
        editTextLayout.setOrientation(LinearLayout.HORIZONTAL);
        ImageView starImage = new ImageView(context);
        starImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        starImage.setImageResource(R.drawable.star_small_inner);
        editTextLayout.addView(starImage, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, Gravity.LEFT | Gravity.CENTER_VERTICAL, 14, 0, 0, 0));
        editTextLayout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1, Gravity.FILL));
        editTextContainer.attachEditText(editText);
        editTextContainer.addView(editTextLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        linearLayout.addView(editTextContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        final LinkSpanDrawable.LinksTextView infoView = new LinkSpanDrawable.LinksTextView(context);
        infoView.setText(AndroidUtilities.replaceArrows(AndroidUtilities.replaceSingleTag(getString(R.string.PaidContentInfo), () -> {
            Browser.openUrl(context, getString(R.string.PaidContentInfoLink));
        }), true));
        infoView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        infoView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        infoView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
        linearLayout.addView(infoView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 14, 3, 14, 24));

        ButtonWithCounterView button = new ButtonWithCounterView(context, resourcesProvider);
        button.setText(getString(stars > 0 ? R.string.PaidContentUpdateButton : R.string.PaidContentButton), false);
        linearLayout.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        final ButtonWithCounterView clearButton;
        if (stars > 0 && allowClear) {
            clearButton = new ButtonWithCounterView(context, false, resourcesProvider);
            clearButton.setText(getString(R.string.PaidContentClearButton), false, false);
            linearLayout.addView(clearButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 4, 0, 0));
        } else {
            clearButton = null;
        }

        b.setCustomView(linearLayout);
        sheet[0] = b.create();

        editText.setText(stars <= 0 ? "" : Long.toString(stars));
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            private boolean ignore;
            private int shakeDp = 2;
            @Override
            public void afterTextChanged(Editable s) {
                if (ignore) return;

                long input_stars = 0;
                try {
                    input_stars = TextUtils.isEmpty(s) ? 0 : Long.parseLong(s.toString());
                    if (input_stars > MessagesController.getInstance(UserConfig.selectedAccount).starsPaidPostAmountMax) {
                        ignore = true;
                        editText.setText(Long.toString(input_stars = MessagesController.getInstance(UserConfig.selectedAccount).starsPaidPostAmountMax));
                        editText.setSelection(editText.getText().length());
                        AndroidUtilities.shakeViewSpring(editTextContainer, shakeDp = -shakeDp);
                    }
                } catch (Exception e) {
                    ignore = true;
                    editText.setText(stars <= 0 ? "" : Long.toString(stars));
                    editText.setSelection(editText.getText().length());
                }
                ignore = false;

                if (!allowClear) {
                    button.setEnabled(input_stars > 0);
                }

                editTextContainer.animateSelection(editText.isFocused(), !TextUtils.isEmpty(editText.getText()));
            }
        });

        final boolean[] loading = new boolean[] { false };
        editText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_NEXT) {
                if (loading[0]) return true;
                if (whenDone != null) {
                    button.setLoading(loading[0] = true);
                    whenDone.run(Long.parseLong(editText.getText().toString()), () -> {
                        AndroidUtilities.hideKeyboard(editText);
                        sheet[0].dismiss();
                    });
                } else {
                    AndroidUtilities.hideKeyboard(editText);
                    sheet[0].dismiss();
                }
                return true;
            }
            return false;
        });
        button.setOnClickListener(v -> {
            if (loading[0]) return;
            if (whenDone != null) {
                String s = editText.getText().toString();
                button.setLoading(loading[0] = true);
                whenDone.run(TextUtils.isEmpty(s) ? 0 : Long.parseLong(s), () -> {
                    AndroidUtilities.hideKeyboard(editText);
                    sheet[0].dismiss();
                });
            } else {
                AndroidUtilities.hideKeyboard(editText);
                sheet[0].dismiss();
            }
        });
        if (clearButton != null) {
            clearButton.setOnClickListener(v -> {
                if (loading[0]) return;
                if (whenDone != null) {
                    clearButton.setLoading(loading[0] = true);
                    whenDone.run(0L, () -> {
                        loading[0] = false;
                        AndroidUtilities.hideKeyboard(editText);
                        sheet[0].dismiss();
                    });
                } else {
                    AndroidUtilities.hideKeyboard(editText);
                    sheet[0].dismiss();
                }
            });
        }

        sheet[0].fixNavigationBar();
        sheet[0].setOnDismissListener(d -> {
            AndroidUtilities.hideKeyboard(editText);
        });
        sheet[0].show();

        BaseFragment lastFragment = LaunchActivity.getLastFragment();
        boolean keyboardVisible = false;
        if (lastFragment instanceof ChatActivity) {
            keyboardVisible = ((ChatActivity) lastFragment).needEnterText();
        }
        AndroidUtilities.runOnUIThread(() -> {
            sheet[0].setFocusable(true);
            editText.requestFocus();
            AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showKeyboard(editText));
        }, keyboardVisible ? 200 : 80);

        return sheet[0];
    }

}
