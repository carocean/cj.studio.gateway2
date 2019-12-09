	$(document).ready(function() {
		$('.table').delegate('.rows>.row','mouseenter mouseleave',function(e) {
			if(e.type=='mouseenter')
			$(this).find('.del').show();
			else
			$(this).find('.del').hide();
		});
		$('.table').delegate('.rows>.row span.del','click',function(e) {
			var id=$(this).parents('li').attr('id');
			$.get('delarticle.service',{id:id},function(data){
				$('.table li[id='+id+']').remove();
			}).error(function(e){
				alert(e.responseText);
			})
		});
		$('.tools > ul > li[news]').on('click',function(e){
			var panel=$('.panel');
			panel.toggle();
		});
		$('.op > span[save]').on('click',function(e){
			var panel=$('.panel');
			var title=panel.find('ul>li[title]>input').val();
			var category=panel.find('ul>li[category]>select>option:selected').val();
			var creator=panel.find('ul>li[creator]>input').val();
			var cnt=panel.find('ul>li[cnt]>textarea').val();
			if(title==''||category==''||creator==''||cnt==''){
				alert('输入的标题、分类、创建者、内容都不能为空');
				return;
			}
			$.post('createarticle.service',{title:title,category:category,creator:creator,cnt:cnt},function(data){
				panel.hide();
				$('.table').html(data);
			}).error(function(e){
				alert(e.responseText);
			});
		});
		$('.op > span[cancel]').on('click',function(e){
			$('.panel').hide();
		});
		$('.tools > ul > li[categories]>select').change(function(e){
			var category=$(this).val();
			$.get('article.html',{category:category},function(data){
				$('.table').html(data);
			}).error(function(e){
				alert(e.responseText);
			});
		});
		$('.rows > .row>.columns>li[title]').on('click',function(e){
			var id=$(this).parents('.row').attr('id');
			window.location.href='articleview.html?article='+id;
		});
		
	});