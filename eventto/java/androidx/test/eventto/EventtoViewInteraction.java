package androidx.test.eventto;


import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewFinder;
import androidx.test.espresso.base.ViewFinderImpl;
import androidx.test.internal.util.Checks;
import androidx.test.platform.view.inspector.WindowInspectorCompat;
import androidx.test.platform.view.inspector.WindowInspectorCompat.ViewRetrievalException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Provider;
import org.hamcrest.Matcher;

public class EventtoViewInteraction {
  // private final Matcher<View> viewMatcher;
  private final ViewFinder viewFinder;
  private long timeoutMillis;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private static long defaultTimeoutMillis = Duration.ofSeconds(5).toMillis();

  EventtoViewInteraction(Matcher<View> viewMatcher) {
    // this.viewMatcher = viewMatcher;
    this.viewFinder = new ViewFinderImpl(viewMatcher, new SimpleRootViewPicker());
    this.timeoutMillis = defaultTimeoutMillis;
  }

  static void setDefaultTimeout(Duration duration) {
    defaultTimeoutMillis = duration.toMillis();
  }

  public EventtoViewInteraction withTimeout(Duration timeout) {
    timeoutMillis = timeout.toMillis();
    return this;
  }

  public void check(ViewAssertion assertion) {
    ViewAssertionCallable viewAssertionCallable = new ViewAssertionCallable(viewFinder, assertion);
    performRetryingTaskOnUiThread(viewAssertionCallable);
  }

  private void performRetryingTaskOnUiThread(Callable<ExecutionStatus> viewInteractionCallable) {
    long startTime = SystemClock.uptimeMillis();
    long remainingTimeout = timeoutMillis;
    long pollDelay = 0;
    while (remainingTimeout > 0) {

      FutureTask<ExecutionStatus> uiTask = new FutureTask<>(viewInteractionCallable);
      mainHandler.postDelayed(uiTask, pollDelay);
      try {
        if (uiTask.get(remainingTimeout, TimeUnit.MILLISECONDS) == ExecutionStatus.SUCCESS) {
          return;
        }
        remainingTimeout = SystemClock.uptimeMillis() - startTime - timeoutMillis;
        pollDelay = 100;
      } catch (InterruptedException ie) {
        throw new RuntimeException("Interrupted running UI task", ie);
      } catch (ExecutionException ee) {
        Throwable cause = ee.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        } else if (cause instanceof Error) {
          throw (Error) cause;
        }
        throw new RuntimeException(cause);
      } catch (TimeoutException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void perform(ViewAction action) {
    ViewActionCallable callable =
        new ViewActionCallable(viewFinder, action, new EventtoUiController());
    performRetryingTaskOnUiThread(callable);
  }

  private static class SimpleRootViewPicker implements Provider<View> {

    @Override
    public View get() {

      try {
        List<View> views = WindowInspectorCompat.getGlobalWindowViews();
        // TODO: handle more than 1 view
        Checks.checkArgument(views.size() <= 1);
        if (views.size() == 1) {
          return views.get(0);
        }
      } catch (ViewRetrievalException e) {
        //
      }
      return null;
    }
  }

  private enum ExecutionStatus {
    SUCCESS,
    RESCHEDULE
  }

  private static class ViewAssertionCallable implements Callable<ExecutionStatus> {
    private final ViewFinder viewFinder;
    private final ViewAssertion viewAssertion;

    ViewAssertionCallable(ViewFinder viewFinder, ViewAssertion viewAssertion) {
      this.viewFinder = viewFinder;
      this.viewAssertion = viewAssertion;
    }

    @Override
    public ExecutionStatus call() throws Exception {
      Log.i("Eventto", "Running ViewAssertionCallable");
      try {
        View matchedView = viewFinder.getView();
        viewAssertion.check(matchedView, null);
        return ExecutionStatus.SUCCESS;
      } catch (NoMatchingViewException e) {
        Log.i("Eventto", "Could not find view, retrying", e);
        return ExecutionStatus.RESCHEDULE;
      }
    }
  }

  private static class ViewActionCallable implements Callable<ExecutionStatus> {
    private final ViewFinder viewFinder;
    private final ViewAction viewAction;
    private final UiController uiController;

    ViewActionCallable(
        ViewFinder viewFinder, EventoViewAction viewAction, UiController uiController) {
      this.viewFinder = viewFinder;
      this.viewAction = viewAction;
      this.uiController = uiController;
    }

    @Override
    public ExecutionStatus call() throws Exception {
      Log.i("Eventto", "Running ViewAssertionCallable");
      try {
        View matchedView = viewFinder.getView();
        viewAction.perform(uiController, matchedView);
        return ExecutionStatus.SUCCESS;
      } catch (NoMatchingViewException e) {
        Log.i("Eventto", "Could not find view, retrying", e);
        return ExecutionStatus.RESCHEDULE;
      }
    }
  }
}
