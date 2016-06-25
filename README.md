# owlet-api

[![Code Climate](https://codeclimate.com/github/codefordenver/owlet-api/badges/gpa.svg)](https://codeclimate.com/github/codefordenver/owlet-api)
[![Issue Count](https://codeclimate.com/github/codefordenver/owlet-api/badges/issue_count.svg)](https://codeclimate.com/github/codefordenver/owlet-api)

**generated using Luminus version "2.9.10.26"**

## SETUP


### Prerequisites:

- Java 1.8 or greater

- You will need [Leiningen][1] 2.0 or above installed.

### Database stuff:

With postgres installed on your system (`brew install postgres`)

- `createdb owlet_dev`
- `createdb owlet_test`
- `touch profiles.clj` (from project root)


- Then populate the contents of `profiles.clj` with:

	```
	{:profiles/dev  {:env {:database-url "jdbc:postgresql://localhost/owlet_dev?user=postgres&password=password"}}
	 :profiles/test {:env {:database-url "jdbc:postgresql://localhost/owlet_test?user=postgres&password=password"}}}
	```
- Migrate all outstanding migrations 
	
	`lein migratus migrate`

	
### Environment Variables:

- export OWLET_CONTENTFUL_MANAGEMENT_AUTH_TOKEN="ask a cfd member"
- export OWLET_CONTENTFUL_DELIVERY_AUTH_TOKEN="ask a cfd member"
- export OWLET_CONTENTFUL_DEFAULT_SPACE_ID="ask a cfd member"
- export OWLET_ACTIVITIES_CONTENTFUL_DELIVERY_AUTH_TOKEN="ask a cfd member"

[1]: https://github.com/technomancy/leiningen

### Running: 

To start a web server for the application, run:

    lein run
    

### Dev Workflow Notes:

During developement you probably want to proxy request from and to `localhost`
to contenful. For this you can use something like [ngrok](https://ngrok.com/)
to tunnel the webhook responses:

with ngrok installed (`brew cask install ngrok`)

then `cd` into **/owlet-api** directory

```
lein run
ngrok http 3000 
```

then finally copy and paste the ngrok url into **contentful.com** webhooks admin page.

