package com.etiennelawlor.moviehub.ui.movies;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.etiennelawlor.moviehub.R;
import com.etiennelawlor.moviehub.data.local.sharedpreferences.PreferencesHelper;
import com.etiennelawlor.moviehub.data.remote.response.Configuration;
import com.etiennelawlor.moviehub.data.remote.response.Movie;
import com.etiennelawlor.moviehub.data.source.movies.MoviesLocalDataSource;
import com.etiennelawlor.moviehub.data.source.movies.MoviesRemoteDataSource;
import com.etiennelawlor.moviehub.data.source.movies.MoviesRepository;
import com.etiennelawlor.moviehub.ui.base.BaseAdapter;
import com.etiennelawlor.moviehub.ui.base.BaseFragment;
import com.etiennelawlor.moviehub.ui.moviedetails.MovieDetailsActivity;
import com.etiennelawlor.moviehub.util.FontCache;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by etiennelawlor on 12/16/16.
 */

public class MoviesFragment extends BaseFragment implements MoviesAdapter.OnItemClickListener, MoviesAdapter.OnReloadClickListener, MoviesContract.View {

    // region Constants
    public static final String KEY_MOVIE = "KEY_MOVIE";
    // endregion

    // region Views
    @BindView(R.id.rv)
    RecyclerView recyclerView;
    @BindView(R.id.error_ll)
    LinearLayout errorLinearLayout;
    @BindView(R.id.error_tv)
    TextView errorTextView;
    @BindView(R.id.pb)
    ProgressBar progressBar;
    @BindView(android.R.id.empty)
    LinearLayout emptyLinearLayout;
    // endregion

    // region Member Variables
    private MoviesAdapter moviesAdapter;
    private Typeface font;
    private Unbinder unbinder;
    private StaggeredGridLayoutManager layoutManager;
    private Configuration configuration;
    private CompositeSubscription compositeSubscription;
    private MoviesContract.Presenter moviesPresenter;
    private View selectedMovieView;
    // endregion

    // region Listeners
    @OnClick(R.id.reload_btn)
    public void onReloadButtonClicked() {
        moviesPresenter.reloadFirstPage();
    }

    private RecyclerView.OnScrollListener recyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(final RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            int visibleItemCount = recyclerView.getChildCount();
            int totalItemCount = recyclerView.getAdapter().getItemCount();
            int[] positions = layoutManager.findFirstVisibleItemPositions(null);
            int firstVisibleItem = positions[1];

            if ((visibleItemCount + firstVisibleItem) >= totalItemCount && totalItemCount > 0) {
                loadMoreItems();
            }
        }
    };
    // endregion

    // region Constructors
    public MoviesFragment() {
    }
    // endregion

    // region Factory Methods
    public static MoviesFragment newInstance() {
        return new MoviesFragment();
    }

    public static MoviesFragment newInstance(Bundle extras) {
        MoviesFragment fragment = new MoviesFragment();
        fragment.setArguments(extras);
        return fragment;
    }
    // endregion

    // region Lifecycle Methods
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        compositeSubscription = new CompositeSubscription();

        moviesPresenter = new MoviesPresenter(
                new MoviesRepository(
                        new MoviesRemoteDataSource(getContext()),
                        new MoviesLocalDataSource(getContext())),
                this);

        font = FontCache.getTypeface("Lato-Medium.ttf", getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_movies, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);

        recyclerView.setLayoutManager(layoutManager);
        moviesAdapter = new MoviesAdapter(getContext());
        moviesAdapter.setOnItemClickListener(this);
        moviesAdapter.setOnReloadClickListener(this);
        recyclerView.setItemAnimator(new SlideInUpAnimator());
        recyclerView.setAdapter(moviesAdapter);

        // Pagination
        recyclerView.addOnScrollListener(recyclerViewOnScrollListener);

        configuration = PreferencesHelper.getConfiguration(getContext());

        if(configuration != null){
            moviesPresenter.loadFirstPage();
        } else {
            moviesPresenter.getConfiguration();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeListeners();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        compositeSubscription.unsubscribe();
    }

    // endregion

    // region MoviesAdapter.OnItemClickListener Methods
    @Override
    public void onItemClick(int position, View view) {
        selectedMovieView = view;
        Movie movie = moviesAdapter.getItem(position);
        if(movie != null){
            moviesPresenter.viewMovieDetails(movie);
        }
    }
    // endregion

    // region MoviesAdapter.OnReloadClickListener Methods
    @Override
    public void onReloadClick() {
        moviesPresenter.reloadNextPage();
    }
    // endregion

    // region MoviesContract.View Methods

    @Override
    public void showEmptyView() {
        emptyLinearLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideEmptyView() {
        emptyLinearLayout.setVisibility(View.GONE);
    }

    @Override
    public void showErrorView() {
        errorLinearLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideErrorView() {
        errorLinearLayout.setVisibility(View.GONE);
    }

    @Override
    public void showLoadingView() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoadingView() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void addFooter() {
        moviesAdapter.addFooter();
    }

    @Override
    public void removeFooter() {
        moviesAdapter.removeFooter();
    }

    @Override
    public void updateFooter(FooterType footerType) {
        switch (footerType){
            case LOAD_MORE:
                moviesAdapter.updateFooter(BaseAdapter.FooterType.LOAD_MORE);
                break;
            case ERROR:
                moviesAdapter.updateFooter(BaseAdapter.FooterType.ERROR);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean isAdapterEmpty() {
        return moviesAdapter.isEmpty();
    }

    @Override
    public void setErrorText(String errorText) {
        errorTextView.setText(errorText);
    }

    @Override
    public void addMoviesToAdapter(List<Movie> movies) {
        moviesAdapter.addAll(movies);
    }

    @Override
    public void saveConfiguration(Configuration configuration) {
        PreferencesHelper.setConfiguration(getContext(), configuration);
    }

    @Override
    public void viewMovieDetails(Movie movie) {
        Intent intent = new Intent(getActivity(), MovieDetailsActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_MOVIE, movie);
//            bundle.putInt(MovieDetailsActivity.KEY_STATUS_BAR_COLOR, getActivity().getWindow().getStatusBarColor());
        intent.putExtras(bundle);

        Window window = getActivity().getWindow();
//            window.setStatusBarColor(ContextCompat.getColor(getContext(), R.color.status_bar_color));

        Resources resources = selectedMovieView.getResources();
        Pair<View, String> moviePair  = getPair(selectedMovieView, resources.getString(R.string.transition_movie_thumbnail));

        ActivityOptionsCompat options = getActivityOptionsCompat(moviePair);

        window.setExitTransition(null);
        ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
    }

    // endregion

    // region Helper Methods
    private void removeListeners() {
        moviesAdapter.setOnItemClickListener(null);
    }

    private void loadMoreItems() {
//        isLoading = true;
//        currentPage += 1;

//        Call getPopularMoviesCall = movieHubService.getPopularMovies(currentPage);
//        calls.add(getPopularMoviesCall);
//        getPopularMoviesCall.enqueue(getPopularMoviesNextFetchCallback);

//        moviesPresenter.loadMovies(currentPage);
        moviesPresenter.loadNextPage();

    }

    private ActivityOptionsCompat getActivityOptionsCompat(Pair pair){
        ActivityOptionsCompat options = null;

        Pair<View, String> bottomNavigationViewPair = getBottomNavigationViewPair();
        Pair<View, String> statusBarPair = getStatusBarPair();
        Pair<View, String> navigationBarPair  = getNavigationBarPair();
        Pair<View, String> appBarPair  = getAppBarPair();

        if(pair!=null
                && bottomNavigationViewPair != null
                && statusBarPair!= null
                && navigationBarPair!= null
                && appBarPair!= null){
            options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                    pair, bottomNavigationViewPair, statusBarPair, navigationBarPair, appBarPair);
        } else if(pair != null
                && bottomNavigationViewPair != null
                && statusBarPair != null
                && appBarPair!= null){
            options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                    pair, bottomNavigationViewPair, statusBarPair, appBarPair);
        } else if(pair != null
                && bottomNavigationViewPair != null
                && navigationBarPair != null
                && appBarPair!= null){
            options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                    pair, bottomNavigationViewPair, navigationBarPair, appBarPair);
        }

        return options;
    }

    private Pair<View, String> getPair(View view, String transition){
        Pair<View, String> posterImagePair = null;
        View posterImageView = ButterKnife.findById(view, R.id.thumbnail_iv);
        if(posterImageView != null){
            posterImagePair = Pair.create(posterImageView, transition);
        }

        return posterImagePair;
    }

    private Pair<View, String> getBottomNavigationViewPair(){
        Pair<View, String> pair = null;
        View bottomNavigationView = ButterKnife.findById(getActivity(), R.id.bottom_navigation);
        if(bottomNavigationView != null) {
            Resources resources = bottomNavigationView.getResources();
            pair = Pair.create(bottomNavigationView, resources.getString(R.string.transition_bottom_navigation));
        }
        return pair;
    }

    private Pair<View, String> getStatusBarPair(){
        Pair<View, String> pair = null;
        View statusBar = ButterKnife.findById(getActivity(), android.R.id.statusBarBackground);
        if(statusBar != null)
            pair = Pair.create(statusBar, statusBar.getTransitionName());
        return pair;
    }

    private Pair<View, String> getNavigationBarPair(){
        Pair<View, String> pair = null;
        View navigationBar = ButterKnife.findById(getActivity(), android.R.id.navigationBarBackground);
        if(navigationBar != null)
            pair = Pair.create(navigationBar, navigationBar.getTransitionName());
        return pair;
    }

    private Pair<View, String> getAppBarPair(){
        Pair<View, String> pair = null;
        View appBar = ButterKnife.findById(getActivity(), R.id.appbar);
        if(appBar != null) {
            Resources resources = appBar.getResources();
            pair = Pair.create(appBar, resources.getString(R.string.transition_app_bar));
        }
        return pair;
    }

    // endregion
}
