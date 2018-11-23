/**
 * @author Sebastian Demian - sebastian.demian@cmegroup.com
 * @since 9/20/18
 */
public class Runner {

  public static void main(String[] args) {

    final String KEY_PATH = "path of the JSON credentials file";
    final String PROJECT_ID = "Google project id";
    final String SUBSCRIPTION_ID = "Pub Sub subscription ID";

    OrderedPubSubListener listener = new OrderedPubSubListener(KEY_PATH, PROJECT_ID, SUBSCRIPTION_ID);

    try {

      listener.start();

    } catch (InterruptedException e) {
      e.printStackTrace();
    }


  }

}
