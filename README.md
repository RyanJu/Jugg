# Jugg
an Android image downloading and caching library writen by Ryan Ju

it's easy to use in your ImageView like : 
Jugg.withContext(context).withUrl(httpUrl).show(imageView);

you can also set place holder ,error holder like:
Jugg.withContext(context).withUrl(httpUrl).errorholder(R.drawable.error).placeholder(R.drawable.loading).show(imageView);

you can get callback by implements the LoadCallback interface and add to the method show(imageView,callback).

