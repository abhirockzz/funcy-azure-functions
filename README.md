## `funcy` - a Serverless Slack app using [Azure Functions](https://azure.microsoft.com/en-in/services/functions/?wt.mc_id=funcyazurefunctions-github-abhishgu)

**funcy** is a Serverless webhook backend running on [Azure Functions](https://azure.microsoft.com/en-in/services/functions/?wt.mc_id=funcyazurefunctions-github-abhishgu). It serves as a trimmed down version of the awesome [Giphy for Slack](https://get.slack.help/hc/en-us/articles/204714258-Giphy-for-Slack). The (original) Giphy Slack app returns a bunch of GIFs for a search term and the user can pick one of them. **funcy** tweaks it a bit by simply returning a (single) random image for a search keyword using the [Giphy Random API](https://developers.giphy.com/docs/#operation--gifs-random-get).

<p align="center">
  <img width="500" height="344" src="https://media1.giphy.com/media/MGdfeiKtEiEPS/giphy-downsized.gif">
</p>