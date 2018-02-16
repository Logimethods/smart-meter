fly -t cicd login --team-name deetazilla-team -c http://ci.logimethodslabs.com:8080

fly -t cicd set-pipeline -p smart_meter-pipeline -c smart_meter-pipeline.yml --load-vars-from  <(docker run --rm logimethods/dz_compose:1.5 export properties/properties.yml) --load-vars-from ../dockerfile-app_compose/properties/properties_app.yml --load-vars-from ../dockerfile-app_compose/properties/properties_branch.yml --load-vars-from credentials.yml
