<%@include file="head.jsp"%><%@include file="header.jsp"%>
<div class="container" id="main">
	<div class="row">
		<%@include file="categorylist.jsp"%>
		<div class="col-sm-6 col-lg-8">
			<h2 class="minipage-title">Sign Up</h2>
			<form action="register" method="POST">
				<div class="row">
					<div class="col-sm-8 col-md-8 col-lg-4">
						<div class="form-group row">
							<label for="username"
								class="col-sm-4 col-form-label col-form-label-lg">Username</label>
							<div class="col-sm-8">
								<input type="text" class="form-control form-control-lg"
									name="username" id="username" placeholder="user" required>
							</div>
						</div>
						<div class="form-group row">
							<label for="password"
								class="col-sm-4 col-form-label col-form-label-lg">Password</label>
							<div class="col-sm-8">
								<input type="password" class="form-control form-control-lg"
									name="password" id="password" placeholder="password" required>
							</div>
						</div>
						<div class="form-group row">
							<label for="email"
								class="col-sm-4 col-form-label col-form-label-lg">Email</label>
							<div class="col-sm-8">
								<input type="email" class="form-control form-control-lg"
									name="email" id="email" placeholder="user@example.com" required>
							</div>
						</div>
						<div class="form-group row">
							<label for="realName"
								class="col-sm-4 col-form-label col-form-label-lg">Real Name</label>
							<div class="col-sm-8">
								<input type="text" class="form-control form-control-lg"
									name="realName" id="realName" placeholder="Jane Doe" required>
							</div>
						</div>
						<input class="btn" name="signup" value="Sign up" type="submit">
					</div>
				</div>
			</form>
		</div>
	</div>
</div>
<%@include file="footer.jsp"%>
